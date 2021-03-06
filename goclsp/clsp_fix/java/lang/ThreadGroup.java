/*
 * This file is modified by Ivan Maidanski <ivmai@ivmaisoft.com>
 * Project name: JCGO (http://www.ivmaisoft.com/jcgo/)
 * Class root location: $(JCGO)/goclsp/clsp_fix
 * Origin: GNU Classpath v0.93
 */

/* ThreadGroup -- a group of Threads
   Copyright (C) 1998, 2000, 2001, 2002, 2005  Free Software Foundation, Inc.

This file is part of GNU Classpath.

GNU Classpath is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2, or (at your option)
any later version.

GNU Classpath is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with GNU Classpath; see the file COPYING.  If not, write to the
Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
02110-1301 USA.

Linking this library statically or dynamically with other modules is
making a combined work based on this library.  Thus, the terms and
conditions of the GNU General Public License cover the whole
combination.

As a special exception, the copyright holders of this library give you
permission to link this library with independent modules to produce an
executable, regardless of the license terms of these independent
modules, and to copy and distribute the resulting executable under
terms of your choice, provided that you also meet, for each linked
independent module, the terms and conditions of the license of that
module.  An independent module is a module which is not derived from
or based on this library.  If you modify this library, you may extend
this exception to your version of the library, but you are not
obligated to do so.  If you do not wish to do so, delete this
exception statement from your version. */

package java.lang;

/**
 * ThreadGroup allows you to group Threads together.  There is a hierarchy
 * of ThreadGroups, and only the initial ThreadGroup has no parent.  A Thread
 * may access information about its own ThreadGroup, but not its parents or
 * others outside the tree.
 *
 * @author John Keiser
 * @author Tom Tromey
 * @author Bryce McKinlay
 * @author Eric Blake (ebb9@email.byu.edu)
 * @see Thread
 * @since 1.0
 * @status updated to 1.4
 */
public class ThreadGroup implements Thread.UncaughtExceptionHandler
{
  /** The Initial, top-level ThreadGroup. */
  static ThreadGroup root = new ThreadGroup();

  /**
   * This flag is set if an uncaught exception occurs. The runtime should
   * check this and exit with an error status if it is set.
   */
  static boolean had_uncaught_exception;

  /** The parent thread group. */
  final ThreadGroup parent;

  /** The group name, non-null. */
  final String name;

  /** The threads in the group. */
  private Thread[] threads = new Thread[4];

  /** Child thread groups, or null when this group is destroyed. */
  private ThreadGroup[] groups = new ThreadGroup[2];

  /** If all threads in the group are daemons. */
  private boolean daemon_flag = false;

  /** The maximum group priority. */
  private int maxpri;

  /**
   * Hidden constructor to build the root node.
   */
  private ThreadGroup()
  {
    name = "main";
    parent = null;
    maxpri = Thread.MAX_PRIORITY;
  }

  /**
   * Create a new ThreadGroup using the given name and the current thread's
   * ThreadGroup as a parent. There may be a security check,
   * <code>checkAccess</code>.
   *
   * @param name the name to use for the ThreadGroup
   * @throws SecurityException if the current thread cannot create a group
   * @see #checkAccess()
   */
  public ThreadGroup(String name)
  {
    this(Thread.currentThread().group, name);
  }

  /**
   * Create a new ThreadGroup using the given name and parent group. The new
   * group inherits the maximum priority and daemon status of its parent
   * group. There may be a security check, <code>checkAccess</code>.
   *
   * @param name the name to use for the ThreadGroup
   * @param parent the ThreadGroup to use as a parent
   * @throws NullPointerException if parent is null
   * @throws SecurityException if the current thread cannot create a group
   * @throws IllegalThreadStateException if the parent is destroyed
   * @see #checkAccess()
   */
  public ThreadGroup(ThreadGroup parent, String name)
  {
    parent.checkAccess();
    this.parent = parent;
    this.name = name;
    maxpri = parent.maxpri;
    daemon_flag = parent.daemon_flag;
    parent.addGroup(this);
  }

  private synchronized void addGroup(ThreadGroup g)
  {
    if (groups == null)
      throw new IllegalThreadStateException();
    int i = groups.length - 2;
    if (groups[i] != null)
      {
        ThreadGroup[] groups2 = new ThreadGroup[(i >> 1) + i + 3];
        VMSystem.arraycopy(groups, 0, groups2, 0, i + 1);
        groups = groups2;
      }
      else
        {
          while (i-- > 0)
            if (groups[i] != null)
              break;
        }
    groups[i + 1] = g;
  }

  /**
   * Get the name of this ThreadGroup.
   *
   * @return the name of this ThreadGroup
   */
  public final String getName()
  {
    return name;
  }

  /**
   * Get the parent of this ThreadGroup. If the parent is not null, there
   * may be a security check, <code>checkAccess</code>.
   *
   * @return the parent of this ThreadGroup
   * @throws SecurityException if permission is denied
   */
  public final ThreadGroup getParent()
  {
    if (parent != null)
      parent.checkAccess();
    return parent;
  }

  /**
   * Get the maximum priority of Threads in this ThreadGroup. Threads created
   * after this call in this group may not exceed this priority.
   *
   * @return the maximum priority of Threads in this ThreadGroup
   */
  public final int getMaxPriority()
  {
    return maxpri;
  }

  /**
   * Tell whether this ThreadGroup is a daemon group.  A daemon group will
   * be automatically destroyed when its last thread is stopped and
   * its last thread group is destroyed.
   *
   * @return whether this ThreadGroup is a daemon group
   */
  public final boolean isDaemon()
  {
    return daemon_flag;
  }

  /**
   * Tell whether this ThreadGroup has been destroyed or not.
   *
   * @return whether this ThreadGroup has been destroyed or not
   * @since 1.1
   */
  public synchronized boolean isDestroyed()
  {
    return groups == null;
  }

  /**
   * Set whether this ThreadGroup is a daemon group.  A daemon group will be
   * destroyed when its last thread is stopped and its last thread group is
   * destroyed. There may be a security check, <code>checkAccess</code>.
   *
   * @param daemon whether this ThreadGroup should be a daemon group
   * @throws SecurityException if you cannot modify this ThreadGroup
   * @see #checkAccess()
   */
  public final void setDaemon(boolean daemon)
  {
    checkAccess();
    daemon_flag = daemon;
  }

  /**
   * Set the maximum priority for Threads in this ThreadGroup. setMaxPriority
   * can only be used to reduce the current maximum. If maxpri is greater
   * than the current Maximum of the parent group, the current value is not
   * changed. Otherwise, all groups which belong to this have their priority
   * adjusted as well. Calling this does not affect threads already in this
   * ThreadGroup. There may be a security check, <code>checkAccess</code>.
   *
   * @param maxpri the new maximum priority for this ThreadGroup
   * @throws SecurityException if you cannot modify this ThreadGroup
   * @see #getMaxPriority()
   * @see #checkAccess()
   */
  public final void setMaxPriority(int maxpri)
  {
    if (maxpri < Thread.MIN_PRIORITY)
      maxpri = Thread.MIN_PRIORITY;
    ThreadGroup[] groups2;
    synchronized (this)
      {
        checkAccess();
        if (maxpri > Thread.MAX_PRIORITY)
          return;
        if (parent != null && maxpri > parent.maxpri)
          maxpri = parent.maxpri;
        this.maxpri = maxpri;
        if (groups == null)
          return;
        groups2 = new ThreadGroup[groups.length];
        VMSystem.arraycopy(groups, 0, groups2, 0, groups2.length);
      }
    ThreadGroup g;
    for (int i = 0; (g = groups2[i]) != null; i++)
      g.setMaxPriority(maxpri);
  }

  /**
   * Check whether this ThreadGroup is an ancestor of the specified
   * ThreadGroup, or if they are the same.
   *
   * @param group the group to test on
   * @return whether this ThreadGroup is a parent of the specified group
   */
  public final boolean parentOf(ThreadGroup group)
  {
    while (group != null)
      {
        if (group == this)
          return true;
        group = group.parent;
      }
    return false;
  }

  /**
   * Find out if the current Thread can modify this ThreadGroup. This passes
   * the check on to <code>SecurityManager.checkAccess(this)</code>.
   *
   * @throws SecurityException if the current Thread cannot modify this
   *         ThreadGroup
   * @see SecurityManager#checkAccess(ThreadGroup)
   */
  public final void checkAccess()
  {
    // Bypass System.getSecurityManager, for bootstrap efficiency.
    SecurityManager sm = SecurityManager.current;
    if (sm != null)
      sm.checkAccess(this);
  }

  /**
   * Return an estimate of the total number of active threads in this
   * ThreadGroup and all its descendants. This cannot return an exact number,
   * since the status of threads may change after they were counted; but it
   * should be pretty close. Based on a JDC bug,
   * <a href="http://developer.java.sun.com/developer/bugParade/bugs/4089701.html">
   * 4089701</a>, we take active to mean isAlive().
   *
   * @return count of active threads in this ThreadGroup and its descendants
   */
  public int activeCount()
  {
    int total = 0;
    ThreadGroup[] groups2;
    synchronized (this)
      {
        if (groups == null)
          return 0;
        groups2 = new ThreadGroup[groups.length];
        VMSystem.arraycopy(groups, 0, groups2, 0, groups2.length);
        Thread t;
        for (int i = 0; (t = threads[i]) != null; i++)
          if (t.isAlive())
            total++;
      }
    ThreadGroup g;
    for (int i = 0; (g = groups2[i]) != null; i++)
      total += g.activeCount();
    return total;
  }

  /**
   * Copy all of the active Threads from this ThreadGroup and its descendants
   * into the specified array.  If the array is not big enough to hold all
   * the Threads, extra Threads will simply not be copied. There may be a
   * security check, <code>checkAccess</code>.
   *
   * @param array the array to put the threads into
   * @return the number of threads put into the array
   * @throws SecurityException if permission was denied
   * @throws NullPointerException if array is null
   * @throws ArrayStoreException if a thread does not fit in the array
   * @see #activeCount()
   * @see #checkAccess()
   * @see #enumerate(Thread[], boolean)
   */
  public int enumerate(Thread[] array)
  {
    checkAccess();
    return enumerate(array, 0, true);
  }

  /**
   * Copy all of the active Threads from this ThreadGroup and, if desired,
   * from its descendants, into the specified array. If the array is not big
   * enough to hold all the Threads, extra Threads will simply not be copied.
   * There may be a security check, <code>checkAccess</code>.
   *
   * @param array the array to put the threads into
   * @param recurse whether to recurse into descendent ThreadGroups
   * @return the number of threads put into the array
   * @throws SecurityException if permission was denied
   * @throws NullPointerException if array is null
   * @throws ArrayStoreException if a thread does not fit in the array
   * @see #activeCount()
   * @see #checkAccess()
   */
  public int enumerate(Thread[] array, boolean recurse)
  {
    checkAccess();
    return enumerate(array, 0, recurse);
  }

  /**
   * Get the number of active groups in this ThreadGroup.  This group itself
   * is not included in the count. A sub-group is active if it has not been
   * destroyed. This cannot return an exact number, since the status of
   * threads may change after they were counted; but it should be pretty close.
   *
   * @return the number of active groups in this ThreadGroup
   */
  public int activeGroupCount()
  {
    ThreadGroup[] groups2;
    synchronized (this)
      {
        if (groups == null)
          return 0;
        groups2 = new ThreadGroup[groups.length];
        VMSystem.arraycopy(groups, 0, groups2, 0, groups2.length);
      }
    int total = 0;
    ThreadGroup g;
    int i;
    for (i = 0; (g = groups2[i]) != null; i++)
      total += g.activeGroupCount();
    return total + i;
  }

  /**
   * Copy all active ThreadGroups that are descendants of this ThreadGroup
   * into the specified array.  If the array is not large enough to hold all
   * active ThreadGroups, extra ThreadGroups simply will not be copied. There
   * may be a security check, <code>checkAccess</code>.
   *
   * @param array the array to put the ThreadGroups into
   * @return the number of ThreadGroups copied into the array
   * @throws SecurityException if permission was denied
   * @throws NullPointerException if array is null
   * @throws ArrayStoreException if a group does not fit in the array
   * @see #activeCount()
   * @see #checkAccess()
   * @see #enumerate(ThreadGroup[], boolean)
   */
  public int enumerate(ThreadGroup[] array)
  {
    checkAccess();
    return enumerate(array, 0, true);
  }

  /**
   * Copy all active ThreadGroups that are children of this ThreadGroup into
   * the specified array, and if desired, also all descendents.  If the array
   * is not large enough to hold all active ThreadGroups, extra ThreadGroups
   * simply will not be copied. There may be a security check,
   * <code>checkAccess</code>.
   *
   * @param array the array to put the ThreadGroups into
   * @param recurse whether to recurse into descendent ThreadGroups
   * @return the number of ThreadGroups copied into the array
   * @throws SecurityException if permission was denied
   * @throws NullPointerException if array is null
   * @throws ArrayStoreException if a group does not fit in the array
   * @see #activeCount()
   * @see #checkAccess()
   */
  public int enumerate(ThreadGroup[] array, boolean recurse)
  {
    checkAccess();
    return enumerate(array, 0, recurse);
  }

  /**
   * Stop all Threads in this ThreadGroup and its descendants.
   *
   * <p>This is inherently unsafe, as it can interrupt synchronized blocks and
   * leave data in bad states.  Hence, there is a security check:
   * <code>checkAccess()</code>, followed by further checks on each thread
   * being stopped.
   *
   * @throws SecurityException if permission is denied
   * @see #checkAccess()
   * @see Thread#stop(Throwable)
   * @deprecated unsafe operation, try not to use
   */
  public final void stop()
  {
    if (innerStopSuspend(false))
      Thread.currentThread().stop();
  }

  private boolean innerStopSuspend(boolean isSuspend)
  {
    Thread current = Thread.currentThread();
    boolean hasCurrent = false;
    ThreadGroup[] groups2;
    synchronized (this)
      {
        checkAccess();
        if (groups == null)
          return false;
        groups2 = new ThreadGroup[groups.length];
        VMSystem.arraycopy(groups, 0, groups2, 0, groups2.length);
        Thread t;
        for (int i = 0; (t = threads[i]) != null; i++)
          if (t == current)
            hasCurrent = true;
            else if (isSuspend)
              t.suspend();
              else t.stop();
      }
    ThreadGroup g;
    for (int i = 0; (g = groups2[i]) != null; i++)
      if (g.innerStopSuspend(isSuspend))
        hasCurrent = true;
    return hasCurrent;
  }

  /**
   * Interrupt all Threads in this ThreadGroup and its sub-groups. There may
   * be a security check, <code>checkAccess</code>.
   *
   * @throws SecurityException if permission is denied
   * @see #checkAccess()
   * @see Thread#interrupt()
   * @since 1.2
   */
  public final void interrupt()
  {
    ThreadGroup[] groups2;
    synchronized (this)
      {
        checkAccess();
        if (groups == null)
          return;
        groups2 = new ThreadGroup[groups.length];
        VMSystem.arraycopy(groups, 0, groups2, 0, groups2.length);
        Thread t;
        for (int i = 0; (t = threads[i]) != null; i++)
          t.interrupt();
      }
    ThreadGroup g;
    for (int i = 0; (g = groups2[i]) != null; i++)
      g.interrupt();
  }

  /**
   * Suspend all Threads in this ThreadGroup and its descendants.
   *
   * <p>This is inherently unsafe, as suspended threads still hold locks,
   * which can lead to deadlock.  Hence, there is a security check:
   * <code>checkAccess()</code>, followed by further checks on each thread
   * being suspended.
   *
   * @throws SecurityException if permission is denied
   * @see #checkAccess()
   * @see Thread#suspend()
   * @deprecated unsafe operation, try not to use
   */
  public final void suspend()
  {
    if (innerStopSuspend(true))
      Thread.currentThread().suspend();
  }

  /**
   * Resume all suspended Threads in this ThreadGroup and its descendants.
   * To mirror suspend(), there is a security check:
   * <code>checkAccess()</code>, followed by further checks on each thread
   * being resumed.
   *
   * @throws SecurityException if permission is denied
   * @see #checkAccess()
   * @see Thread#suspend()
   * @deprecated pointless, since suspend is deprecated
   */
  public final void resume()
  {
    ThreadGroup[] groups2;
    synchronized (this)
      {
        checkAccess();
        if (groups == null)
          return;
        groups2 = new ThreadGroup[groups.length];
        VMSystem.arraycopy(groups, 0, groups2, 0, groups2.length);
        Thread t;
        for (int i = 0; (t = threads[i]) != null; i++)
          t.resume();
      }
    ThreadGroup g;
    for (int i = 0; (g = groups2[i]) != null; i++)
      g.resume();
  }

  /**
   * Destroy this ThreadGroup.  The group must be empty, meaning that all
   * threads and sub-groups have completed execution. Daemon groups are
   * destroyed automatically. There may be a security check,
   * <code>checkAccess</code>.
   *
   * @throws IllegalThreadStateException if the ThreadGroup is not empty, or
   *         was previously destroyed
   * @throws SecurityException if permission is denied
   * @see #checkAccess()
   */
  public final void destroy()
  {
    ThreadGroup[] groups2;
    synchronized (this)
      {
        checkAccess();
        if (groups == null || threads[0] != null)
          throw new IllegalThreadStateException();
        groups2 = groups;
        groups = null;
      }
    ThreadGroup g;
    for (int i = 0; (g = groups2[i]) != null; i++)
      g.destroy();
    if (parent != null)
      parent.removeGroup(this);
  }

  /**
   * Print out information about this ThreadGroup to System.out. This is
   * meant for debugging purposes. <b>WARNING:</b> This method is not secure,
   * and can print the name of threads to standard out even when you cannot
   * otherwise get at such threads.
   */
  public void list()
  {
    list("");
  }

  /**
   * When a Thread in this ThreadGroup does not catch an exception, the
   * virtual machine calls this method. The default implementation simply
   * passes the call to the parent; then in top ThreadGroup, it will
   * ignore ThreadDeath and print the stack trace of any other throwable.
   * Override this method if you want to handle the exception in a different
   * manner.
   *
   * @param thread the thread that exited
   * @param t the uncaught throwable
   * @throws NullPointerException if t is null
   * @see ThreadDeath
   * @see System#err
   * @see Throwable#printStackTrace()
   */
  public void uncaughtException(Thread thread, Throwable t)
  {
    Thread.UncaughtExceptionHandler ueh;
    if (parent != null)
      parent.uncaughtException(thread, t);
    else if ((ueh = Thread.getDefaultUncaughtExceptionHandler()) != null)
      ueh.uncaughtException(thread, t);
    else if (! (t instanceof ThreadDeath))
      {
        if (t == null)
          throw new NullPointerException();
        had_uncaught_exception = true;
        try
          {
            if (thread != null)
              {
                synchronized (System.err)
                  {
                    System.err.print("Exception in thread \"" +
                                thread.name + "\" ");
                    t.printStackTrace(System.err);
                  }
              }
            else
              t.printStackTrace(System.err);
          }
        catch (ThreadDeath death)
          {
            throw death;
          }
        catch (Throwable x)
          {
            // This means that something is badly screwed up with the runtime,
            // or perhaps someone overloaded the Throwable.printStackTrace to
            // die. In any case, try to deal with it gracefully.
            try
              {
                System.err.println(t.toString());
                System.err.println("*** Got " + x
                                   + " while trying to print stack trace.");
              }
            catch (ThreadDeath death)
              {
                throw death;
              }
            catch (Throwable x2)
              {
                // Here, someone may have overloaded t.toString() or
                // x.toString() to die. Give up all hope; we can't even chain
                // the exception, because the chain would likewise die.
                System.err.println("*** Catastrophic failure while handling "
                                   + "uncaught exception.");
                throw new InternalError();
              }
          }
      }
  }

  /**
   * Originally intended to tell the VM whether it may suspend Threads in
   * low memory situations, this method was never implemented by Sun, and
   * is hence a no-op.
   *
   * @param allow whether to allow low-memory thread suspension; ignored
   * @return false
   * @since 1.1
   * @deprecated pointless, since suspend is deprecated
   */
  public boolean allowThreadSuspension(boolean allow)
  {
    return false;
  }

  /**
   * Return a human-readable String representing this ThreadGroup. The format
   * of the string is:<br>
   * <code>getClass().getName() + "[name=" + getName() + ",maxpri="
   * + getMaxPriority() + ']'</code>.
   *
   * @return a human-readable String representing this ThreadGroup
   */
  public String toString()
  {
    return getClass().getName() + "[name=" + name + ",maxpri=" + maxpri + "]";
  }

  /**
   * Implements enumerate.
   *
   * @param list the array to put the threads into
   * @param next the next open slot in the array
   * @param recurse whether to recurse into descendent ThreadGroups
   * @return the number of threads put into the array
   * @throws SecurityException if permission was denied
   * @throws NullPointerException if list is null
   * @throws ArrayStoreException if a thread does not fit in the array
   * @see #enumerate(Thread[])
   * @see #enumerate(Thread[], boolean)
   */
  private int enumerate(Thread[] list, int next, boolean recurse)
  {
    if (list.length > next)
      {
        ThreadGroup[] groups2;
        synchronized (this)
          {
            if (groups == null)
              return next;
            Thread t;
            for (int i = 0; (t = threads[i]) != null; i++)
              if (t.isAlive())
                {
                  list[next++] = t;
                  if (list.length == next)
                    return next;
                }
            if (!recurse)
              return next;
            groups2 = new ThreadGroup[groups.length];
            VMSystem.arraycopy(groups, 0, groups2, 0, groups2.length);
          }
        ThreadGroup g;
        for (int i = 0; (g = groups2[i]) != null; i++)
          next = g.enumerate(list, next, true);
      }
    return next;
  }

  /**
   * Implements enumerate.
   *
   * @param list the array to put the groups into
   * @param next the next open slot in the array
   * @param recurse whether to recurse into descendent ThreadGroups
   * @return the number of groups put into the array
   * @throws SecurityException if permission was denied
   * @throws NullPointerException if list is null
   * @throws ArrayStoreException if a group does not fit in the array
   * @see #enumerate(ThreadGroup[])
   * @see #enumerate(ThreadGroup[], boolean)
   */
  private int enumerate(ThreadGroup[] list, int next, boolean recurse)
  {
    if (list.length > next)
      {
        ThreadGroup[] groups2;
        ThreadGroup g;
        synchronized (this)
          {
            if (groups == null)
              return next;
            for (int i = 0; (g = groups[i]) != null; i++)
              {
                list[next++] = g;
                if (list.length == next)
                  return next;
              }
            if (!recurse)
              return next;
            groups2 = new ThreadGroup[groups.length];
            VMSystem.arraycopy(groups, 0, groups2, 0, groups2.length);
          }
        for (int i = 0; (g = groups2[i]) != null; i++)
          next = g.enumerate(list, next, true);
      }
    return next;
  }

  /**
   * Implements list.
   *
   * @param indentation the current level of indentation
   * @see #list()
   */
  private void list(String indentation)
  {
    Thread[] threads2;
    ThreadGroup[] groups2;
    synchronized (this)
      {
        if (groups == null)
          return;
        groups2 = new ThreadGroup[groups.length];
        VMSystem.arraycopy(groups, 0, groups2, 0, groups2.length);
        threads2 = new Thread[threads.length];
        VMSystem.arraycopy(threads, 0, threads2, 0, threads2.length);
      }
    System.out.println(indentation + toString());
    indentation += " " + " " + " " + " ";
    Thread t;
    for (int i = 0; (t = threads2[i]) != null; i++)
      System.out.println(indentation + t.toString());
    ThreadGroup g;
    for (int i = 0; (g = groups2[i]) != null; i++)
      g.list(indentation);
  }

  /**
   * Add a thread to the group. Called by Thread constructors.
   *
   * @param t the thread to add, non-null
   * @throws IllegalThreadStateException if the group is destroyed
   */
  final synchronized void addThread(Thread t)
  {
    if (groups == null)
      throw new IllegalThreadStateException("ThreadGroup is destroyed");
    int i = threads.length - 2;
    if (threads[i] != null)
      {
        Thread[] threads2 = new Thread[(i >> 1) + i + 3];
        VMSystem.arraycopy(threads, 0, threads2, 0, i + 1);
        threads = threads2;
      }
      else
        {
          while (i-- > 0)
            if (threads[i] != null)
              break;
        }
    threads[i + 1] = t;
  }

  /**
   * Called by the VM to remove a thread that has died.
   *
   * @param t the thread to remove, non-null
   * @XXX A ThreadListener to call this might be nice.
   */
  final synchronized void removeThread(Thread t)
  {
    if (groups == null)
      return;
    Thread t2;
    int i = 0;
    while ((t2 = threads[i++]) != null)
      if (t2 == t)
        {
          VMSystem.arraycopy(threads, i, threads, i - 1, threads.length - i);
          break;
        }
    t.group = null;
    // Daemon groups are automatically destroyed when all their threads die.
    if (daemon_flag && groups[0] == null && threads[0] == null)
      {
        // We inline destroy to avoid the access check.
        groups = null;
        if (parent != null)
          parent.removeGroup(this);
      }
  }

  /**
   * Called when a group is destroyed, to remove it from its parent.
   *
   * @param g the destroyed group, non-null
   */
  final synchronized void removeGroup(ThreadGroup g)
  {
    if (groups == null)
      return;
    ThreadGroup g2;
    int i = 0;
    while ((g2 = groups[i++]) != null)
      if (g2 == g)
        {
          VMSystem.arraycopy(groups, i, groups, i - 1, groups.length - i);
          break;
        }
    // Daemon groups are automatically destroyed when all their threads die.
    if (daemon_flag && groups[0] == null && threads[0] == null)
      {
        // We inline destroy to avoid the access check.
        groups = null;
        if (parent != null)
          parent.removeGroup(this);
      }
  }

  /*
   * Helper method for the VM. Find a Thread by its Id.
   *
   * @param id The Thread Id.
   * @return Thread object or null if thread doesn't exist.
   */
  static Thread getThreadFromId(long id)
  {
    return root.getThreadFromIdImpl(id);
  }

  private Thread getThreadFromIdImpl(long id)
  {
    ThreadGroup[] groups2;
    Thread t;
    synchronized (this)
      {
        if (groups == null)
          return null;
        for (int i = 0; (t = threads[i]) != null; i++)
          if (t.getId() == id)
            return t;
        groups2 = new ThreadGroup[groups.length];
        VMSystem.arraycopy(groups, 0, groups2, 0, groups2.length);
      }
    ThreadGroup g;
    for (int i = 0; (g = groups2[i]) != null; i++)
      if ((t = g.getThreadFromIdImpl(id)) != null)
        break;
    return t;
  }
} // class ThreadGroup
