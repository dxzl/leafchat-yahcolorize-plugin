/*
 Copyright 2015 Scott Swift - This program is distributed under the
 terms of the GNU General Public License. (Plus see the LeafChat License)
*/
package com.leafdigital.colorize;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import com.leafdigital.colorize.CDS;
import com.leafdigital.colorize.ColorizePlugin.User32;
import com.sun.jna.platform.win32.WinDef.*;
import com.sun.jna.*;

import leafchat.core.api.*;

public class ColorizeMsgPump
{
  public Pointer hWindow = null;
  public Pointer hComponent = null;
  public HWND hColorize = null;
	
  private ColorizePlugin plugin;
  private ColorizeMsgPump msgPump;
	
  protected static User32.WindowProc proc = null;
  
  private JFrame frame = null;
	
  List<RxListener> listeners = new ArrayList<RxListener>();

  ColorizeMsgPump(PluginContext context)
  {
		plugin=(ColorizePlugin)context.getPlugin();

		msgPump = this; // save to pass to the event handler from WndProc!

  	try
  	{
      frame = new JFrame();
//      frame.setName(""); does this set the classname? SunAwtFrame is default      
      frame.setTitle("ColorizeMsgPump"); // need this to be found by FindWindow      
  	  frame.setLocation(-100000, -100000);
      frame.setEnabled(false);
      frame.setVisible(true); // Have to show then hide it...
  	  frame.setVisible(false);
      
  		// Set "handle" to the JFrame window's handle
      this.hComponent = Native.getWindowPointer(frame);      
  	} 
  	catch (Exception e)
  	{
  		e.printStackTrace();
  	}
        
    try
    {
      proc = new User32.WindowProc()
      {
        public LRESULT callback(HWND hWnd, int msg, HWND wParam, LPARAM lParam)
        {
//          if (msg == User32.WM_QUERYENDSESSION)
//          {
//            return new LRESULT(0);
//          }
          if (msg == User32.WM_COPYDATA)
          {
            // wParam will have the YahCoLoRiZe main window-handle,
            // lParam will point to a COPYDATASTRUCT with dwData of 4 bytes,
            // cbData of 4 bytes and lpData is a pointer of machine-dependent
            // length.
            if (wParam != null && lParam != null)
            {   
              CDS cds = new CDS(lParam);

              if (cds.lpData != 0 && cds.cbData != 0)
              {
                Memory m = new Memory(cds.cbData);
                m.write(0, cds.data, 0, cds.cbData);

                CNS cns = new CNS();
                
                int idx = 0;
                
                if (Platform.is64Bit())
                {                  
                  cns.lspare = m.getInt(idx); idx += 8;
                  cns.clientID = m.getShort(idx); idx += 4; // Enforced on receive!
                  cns.commandID = m.getShort(idx); idx += 4;
                  cns.commandData = m.getShort(idx); idx += 4;
                  cns.serverID = m.getShort(idx); idx += 4;
                  cns.channelID = m.getShort(idx); idx += 4;
                  
                  cns.lenChanNick = m.getShort(idx); idx += 4;                  
                  if (cns.lenChanNick > ColorizePlugin.CNS_CHANNICKLEN) // limit
                    cns.lenChanNick = ColorizePlugin.CNS_CHANNICKLEN;
                  
                  cns.lenData = m.getShort(idx); idx += 4;                  
                  if (cns.lenData > ColorizePlugin.CNS_DATALEN) // limit
                    cns.lenData = ColorizePlugin.CNS_DATALEN;
                  
                  cns.chanNick = m.getByteArray(idx, cns.lenChanNick);                  
                  idx += ColorizePlugin.CNS_CHANNICKLEN;
                  
                  cns.data = m.getByteArray(idx, cns.lenData);            
                  //idx += ColorizePlugin.CNS_DATALEN;
                }
                else
                {                  
                  cns.lspare = m.getLong(idx); idx += 8;
                  cns.clientID = m.getInt(idx); idx += 4; // Enforced on receive!
                  cns.commandID = m.getInt(idx); idx += 4;
                  cns.commandData = m.getInt(idx); idx += 4;
                  cns.serverID = m.getInt(idx); idx += 4;
                  cns.channelID = m.getInt(idx); idx += 4;
                  
                  cns.lenChanNick = m.getInt(idx); idx += 4;
                  if (cns.lenChanNick > ColorizePlugin.CNS_CHANNICKLEN) // limit
                    cns.lenChanNick = ColorizePlugin.CNS_CHANNICKLEN;
                  
                  cns.lenData = m.getInt(idx); idx += 4;
                  if (cns.lenData > ColorizePlugin.CNS_DATALEN) // limit
                    cns.lenData = ColorizePlugin.CNS_DATALEN;
                  
                  cns.chanNick = m.getByteArray(idx, cns.lenChanNick);  
                  idx += ColorizePlugin.CNS_CHANNICKLEN;
                  
                  cns.data = m.getByteArray(idx, cns.lenData);            
                  //idx += ColorizePlugin.CNS_DATALEN;
                }

                
                // Set our handle
                hColorize = wParam;
                
                // Fire event subscribed to by ColorizePlugin
                if (cns.clientID == ColorizePlugin.LEAFCHAT_ID)
                  rxDataAvailable(msgPump, plugin, cns);
              }
            }
            return new LRESULT(0);
          }
          
          return User32.INSTANCE.DefWindowProcW(hWnd, msg, wParam, lParam);
        }
      };

      User32.INSTANCE.SetWindowLongW(this.hComponent, User32.GWL_WNDPROC, proc);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
	
  public void addListener(RxListener toAdd)
  {
    listeners.add(toAdd);
  }

  /**
   * Works in conjunction with RxListener.java to signal a
   * WM_COPYDATA received event.
   * 
   * @param <none>
   */  
  public void rxDataAvailable(ColorizeMsgPump msgpump, ColorizePlugin plugin, CNS cns)
  {
    // Notify everybody that may be interested.
    for (RxListener hl : listeners)
      hl.gotRx(msgpump, plugin, cns);
  }
  
  void focus()
  {
		frame.requestFocus();
	}
	
	void close()
	{
		frame.dispose();
	}
	
	void windowClosed()
	{
		plugin.windowClosed();
	}
}
