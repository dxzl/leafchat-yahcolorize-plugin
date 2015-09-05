/*
 Copyright 2015 Scott Swift - This program is distributed under the
 terms of the GNU General Public License. (Plus see the LeafChat License)
*/
/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and Others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Takashi ITOH - initial API and implementation
 *
 * http://eclipsesrc.appspot.com/jsrcs/org.eclipse.actf/org.eclipse.actf.common/plugins/org.eclipse.actf.util.win32/src/org/eclipse/actf/util/win32/COPYDATASTRUCT.java.html
 *
 *******************************************************************************/
package com.leafdigital.colorize;

import com.leafdigital.colorize.ColorizePlugin.Kernel32;
import com.sun.jna.Memory;
import com.sun.jna.Platform;
import com.sun.jna.platform.win32.WinDef.LPARAM;

/**
 * Utility class to send/receive messages with other window
 */
//@SuppressWarnings("restriction")
public class CDS
{
  public static final int WM_COPYDATA = 0x4a;

  public int m_size;
  
  public int dwData;
  public int cbData;
  public int lpData;
  public byte[] data;

  /**
   * Create a COPYDATASTRUCT object using lParam
   * 
   * @param lParam
   */
  public CDS(LPARAM lParam)
  {
    m_size = ColorizePlugin.CDS_SIZE; // must be 12 for either 32 or 64 bit Java!
    
    if (lParam != null)
    {
      Memory buf = new Memory(m_size);

      Kernel32 k32 = Kernel32.INSTANCE;
      
      k32.RtlMoveMemory(buf, lParam, m_size);

      if (Platform.is64Bit())
      {
        dwData = buf.getShort(0);
        cbData = buf.getShort(4);
        lpData = buf.getShort(8);
      }
      else
      {
        dwData = buf.getInt(0);
        cbData = buf.getInt(4);
        lpData = buf.getInt(8);        
      }
      
      if (lpData != 0 && cbData > 0)
      {
        data = new byte[cbData];
        k32.RtlMoveMemory(data, lpData, cbData);
      }
      else
      {
        data = new byte[0];
        cbData = 0;
      }
    }
    else
    {
      data = new byte[0];
    }
  }
}

