/*
 * @(#) $(JCGO)/minihdr/common/stdint.h --
 * a part of the minimalist "libc" headers for JCGO.
 **
 * Project: JCGO (http://www.ivmaisoft.com/jcgo/)
 * Copyright (C) 2001-2008 Ivan Maidanski <ivmai@ivmaisoft.com>
 * All rights reserved.
 */

/*
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 **
 * This software is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License (GPL) for more details.
 **
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library. Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 **
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent
 * modules, and to copy and distribute the resulting executable under
 * terms of your choice, provided that you also meet, for each linked
 * independent module, the terms and conditions of the license of that
 * module. An independent module is a module which is not derived from
 * or based on this library. If you modify this library, you may extend
 * this exception to your version of the library, but you are not
 * obligated to do so. If you do not wish to do so, delete this
 * exception statement from your version.
 */

#ifndef _STDINT_H
#define _STDINT_H

#ifndef _STDDEF_H
#include <stddef.h>
#endif

#ifdef __cplusplus
extern "C"
{
#endif

#ifndef _INTPTR_T_DEFINED
#define _INTPTR_T_DEFINED
#ifdef _LLP64
typedef ptrdiff_t intptr_t;
#else
typedef int intptr_t;
#endif
#endif

#ifndef _UINTPTR_T_DEFINED
#define _UINTPTR_T_DEFINED
#ifdef _LLP64
typedef unsigned __int64 uintptr_t;
#else
typedef unsigned long uintptr_t; /* or unsigned */
#endif
#endif

#ifdef __cplusplus
}
#endif

#endif
