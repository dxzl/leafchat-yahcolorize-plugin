---------------------------------------------------------------------------
Copyright 2015 Scott Swift - This program is distributed under the
terms of the GNU General Public License. (Plus see the LeafChat License)
---------------------------------------------------------------------------

To build my plugin be prepared to spend some time. If you are not a Java developer, forget it (or it may take a while...)

You are going to need to get Eclipse:
http://www.eclipse.org/downloads/

Get LeafChat and all the plugins for it:
https://github.com/quen/leafchat
http://www.leafdigital.com/software/leafchat/

java Native Access:
https://github.com/java-native-access/jna/

java SDK:
http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html

I didn't want to post my ENTIRE Eclipse project-path on GitHub, with JNA already piece-wise installed (it's HUGE!),
just for one plugin... but e-mail me (dxzl@live.com) and I'll zip it up for you and send it.
--------------------------------------------------------------------------------------------------------------------------

S.S. 6/24/2015 - Got it to run with the new SDK and Java 8 on Win XP - to do this I found I had to copy new versions
of java from the SDK into Windows System32... copy java.exe,  javacpl.exe, javaw.exe, javaws.exe, javacpl.cpl.
(It may be ok to just delete these files from System32 also - so we know we are invoking the newest version!)

To build the main leafChat.jar file:
S.S. 12/24/2013 - To build the main leafChat.jar file:

Note: in Environment Variables add "C:\Program Files\Java\jdk1.8.0_45\bin;" to the "Path" to access
the executable jar tools from "Command Prompt"

Do a regular install of the original leafchat client. Copy the C:\Program Files\leafChat 2\leafChat.jar
file to the desktop and rename it leafChat.zip. Now extract the zip file folder "leafchat" into
a new folder you create C:\leafChatJARs\MainJAR.

Copy the C:\Documents and Settings\Owner\My Documents\Eclipse\workspace\LeafChat\bin\leafchat directory
into C:\leafChatJARs\MainJAR to overwrite the class files we have access to and were able to build but keeping any
files in the original distribution that we don't have (It should have two subdirectories, "core" and "startup").

To create leafChat.jar in C\leafChatJAR, run Windows command-prompt and type:
"cd \", "cd leafChatJARs\MainJAR". Now copy, then right-click Paste the following command:

jar cfe leafChat.jar leafchat.startup.main ./leafchat/

*note the above command will create a manifest file for the leafchat.startup.main class entry point automatically.

PROBLEM - if you are using a newer version of the JDK and automatically create a manifest it may not run
with mixed class files in other jars from an earlier version.  So I found I had to build the original manifest file:

YOU MUST RUN THIS BEFORY COPYING leafchat.jar!

jar cfm leafChat.jar ./META-INF/manifest.mf ./leafchat/

*note: here is the original manifest.mf file that needs to be in directory META-INF:

Manifest-Version: 1.0
Ant-Version: Apache Ant 1.8.2
Created-By: 1.6.0_33-b03-424-10M3720 (Apple Inc.)
Main-Class: leafchat.startup.Main

Now copy leafChat.jar back into the leafChat 2 directory and it will run in the Java VM :-)

----------------------------------------------------------------------
NOTE: In building my own version of LeafChat, I had to modify SystemVersion.java to replace
@TITLEBARVERSION@ @CTCPVERSION@ and @BUILDVERSION@ with "2.5" (or whatever)
----------------------------------------------------------------------

-------------------------------------------------------------------------------------------------
To Build The YahCoLoRiZe Plugin For Leafchat: colorize.jar,
-------------------------------------------------------------------------------------------------
To get JNA (Java Native Access): https://github.com/twall/jna

Get jna.jar

https://maven.java.net/content/repositories/releases/net/java/dev/jna/jna/4.1.0/jna-4.1.0.jar

and jna-platform.jar

https://maven.java.net/content/repositories/releases/net/java/dev/jna/jna-platform/4.1.0/jna-platform-4.1.0.jar

Rename them to .zip and you can copy the .class files to the same directory tree in Colorize Plugin's
"lib\com\sun\jna" files. Just delete the directories that are for non-win32 platforms.
-------------------------------------------------------------------------------------------------
Ok to build the leafChat Plugin for YahCoLoRiZe: Install Eclipse and LeafChat. Copy the
YahColorize Plugin to the Eclipse "workspace" and Import it into Eclipse as a project.
You may have to import it from the desktop and let Eclipse copy some of the files...
Install the Sun Java SDK.

1) Unzip ColorizeJAR into C:\leafChatJARs\ColorizeJAR

2) Put the folder shortcuts on the desktop.
Folder 1 "C:\Program Files (x86)\leafChat 2\plugins"
Folder 2 "C:\LeafChatJARs\ColorizeJAR"
Folder 3: "C:\Users\Scott\Eclipse\workspace\Colorize Plugin\bin\com"

3) Add the jar.exe path to your PATH environment-variable (Don't open the command-prompt until after this!):
(add this line, no quotes: "C:\Program Files (x86)\Java\jdk1.8.0_45\bin";)

4) After the project is built in Eclipse, open Folders 1,2,3 and the Command Prompt.

Open Folder 3 and copy the leafChat folder and paste it into Folder 2's "com" folder.

5) Open the command prompt to C:\leafChatJARs\ColorizeJAR and copy/paste this
command string into the Command Prompt window:

For Release 1.4 and up:
  jar.exe cfm colorize.jar .\META-INF\manifest.mf ./com/
  
For Release 1.1-1.3:
  jar.exe cfm colorize.jar .\META-INF\manifest.mf ./com/ ./org/

6) Hit return to build the colorize.jar file. it will appear in Folder 2.

7) Drag colorize.jar into Folder 1 (which points to the LeafChat installation's "plugins"
directory).

8) Now you can run LeafChat and it will load our plugin for testing :-)

Scott Swift 01/17/2014

--------------------------------------------------------------------------------------------------------------------------------
BELOW IS FROM AN OLDER README I WROTE... FYI
--------------------------------------------------------------------------------------------------------------------------------

To Build The YahCoLoRiZe Plugin For Leafchat: colorize.jar,
-------------------------------------------------------------------------------------------------

colorize.jar is our plugin for leafChat. Copy it to Program Files\leafChat\plugins.

First I compile the project using Eclipse "Colorize Plugin" project, (do a Clean and it auto-compiles)
then copy the com\leafdigital\colorize directory structure into C:\leafChatJARs\ColorizeJAR\com, then I
copied sun\jna\(all files and subdirectoried) into C:\leafChatJARs\ColorizeJAR\com. By trial/error I weeded
out class files I don't use and zipped that in ColorizeJAR_partialJNA.zip. 

So you have C:\ColorizeJAR->
                         com->
                              sun->
                                   jna->(etc)
                              leafdigital->
                                           colorize->(etc)
                         org->
                              eclipse->
                                       swt->
                                            internal->(etc)
                                            (etc)

So now I open a command-prompt, navigate into ColorizeJAR, CTRL-C the following line
and right-click paste:

cd C:\leafChatJARs\ColorizeJAR

For Release 1.4 and up:
  "C:\Program Files\Java\jdk1.8.0_45\bin\jar.exe" cfm colorize.jar .\META-INF\manifest.mf ./com/
  
For Release 1.1-1.3:
  "C:\Program Files\Java\jdk1.8.0_45\bin\jar.exe" cfm colorize.jar .\META-INF\manifest.mf ./com/ ./org/

or 

For Release 1.4 and up: 
  jar cfm colorize.jar .\META-INF\manifest.mf ./com/
  
For Release 1.1-1.3:
  jar cfm colorize.jar .\META-INF\manifest.mf ./com/ ./org/

-------------------------------------------------------------------------------------------------
YET MORE INFO - To make a shortcut to force leafchat and jna to launch in the 32-bit JVM:
(NOTE You may need to delete old versions of javaw.exe from C:\Windows\SysWOW64 and/or
C:\Windows\System32 because a rogue version may be running. The default java launch shortcut
points to C:\ProgramData\Oracle\Java\javapath\javaw.exe -jar leafChat.jar the shortcut in
that directory may not be what we want...
-------------------------------------------------------------------------------------------------

The YahCoLoRiZe plugin requires 32-bit Java! Go to: http://www.java.com/en/download/faq/java_win64bit.xml
and click the link to install 32-bit Java on your 64-bit machine!

You will also need to create a shortcut on your desktop to launch LeafChat in 32-bit mode:

1. Right-click on the desktop and choose New->Shortcut

2. In "Target" put this exactly, keeping the quotes as they are:

       "C:\Program Files (x86)\Java\jre1.8.0_45\bin\javaw.exe" -jar -d32 leafChat.jar
       
3. In "Start In" put this (with the quotes):

       "C:\Program Files (x86)\leafChat 2\"
       
4. Click OK then right-click the new shortcut and Rename it to "LeafChat"
(The 1.8.0_45 will change... so check in C:\Program Files (x86)\Java for your version and use that.)
-------------------------------------------------------------------------------------------------
