import java.util.*;

import javax.mail.*;
import javax.mail.event.*;

import com.sun.mail.imap.*;

import java.awt.AWTException;
import java.awt.CheckboxMenuItem;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;

import javax.crypto.*;
import javax.crypto.spec.*;
import javax.xml.bind.DatatypeConverter;

import java.io.File;
//import java.awt.event.KeyEvent;
//import java.io.*;

/* to exporting jar from eclipse,  
 * "runnable jar file", filename: gmonitor.jar
  "copy required libraries into sub-folder next to generated jar" 
  after export 
    1) you can delete the "gmonitor_lib" directory that contains mail47.jar  
    2) open jar in 7zip and modify as follows:
      1) remove non gmonitor classes from root directory
      2) need to update META-INF\MANIFEST.MF to remove "gmonitor_lib/" leaving the line as "Class-Path: . mail-1.4.7.jar"


  play sound http://stackoverflow.com/questions/2416935/how-to-play-wav-files-with-java

  original code forked from http://shoudaw.host22.com/?p=269

*/

   

public class gmonitor {
	private static String CONFIGFILE="gmonitor.ini";
	public static final String CHECKFILE = "c:\\t\\sarta.mail"; 
	private static final String BUILDDATE="4/24/2015";
	private static final int MAXHISTORY = 45;
	private static StringBuilder history;
	private static boolean balloonmode = true;
	private static boolean alertsEnabled = true;
	private static TrayIcon trayIcon;
	private static String iconset = ""; // blank or "2" for alternate icons 
	private static String username = "";
	private static String cleartextpw = "";
	private static String host = "imap.gmail.com";
	private static String mailstring;
	private static boolean proxymode = true;
	private static String connectionStatus = "Uninitialized";
	private static int freq = 250;
	private static String folderStr = "Inbox";
	private static String emptytooltip = "configuring...";
	private static String tooltip;
	private static Store store;
	private static List<String> filters = new ArrayList<String>(); 
	private static boolean mailempty = true;
	private static int historycount = 0;
	private static final String ALGORITHM = "Blowfish";
	private static final String b64key = "itZbdUHhGg==";
	private static final String CHARSET = "UTF-8";
	private static boolean showConAlerts = false;
	private static boolean skipconfigfile = false;
	
	private static Image newmailicon; // = Toolkit.getDefaultToolkit().getImage("C:/t/newmailicon.jpg");
	private static Image nomailicon;
	private static Image disconnectedicon;
//   public static String wavfilename1 = "/resources/notify2.wav";

	public static void main(String argv[])  {
		
		if (argv.length > 0) {
			// for now only 1 arg will be either the config filename, "skipconfigfile", or "help"
			if (argv[0].matches("help")) {
				System.out.println("configfile params:  username, encryptedpassword, cleartextpassword, balloonmode");
				System.out.println("proxy(=proxyport), filter(=str1,str2), iconset=2");
				System.out.println("at command line: skipconfigfile, configfilename");
				System.exit(0);
			} else if (argv[0].matches("skipconfigfile")) {
				skipconfigfile = true;
			} else {
				CONFIGFILE=argv[0];
			}
				
		}
			
		try {	
//	      gmonitor mymonitor = new gmonitor();
	      new clearIconWatchdog().start();
	      new gmonitor().Run();
//	      mymonitor.Run();
	      
		} catch (Exception e) {
			javax.swing.JOptionPane.showMessageDialog(null, e.getStackTrace() );
		}
   }
	
	@SuppressWarnings("deprecation")
	public void Run() throws AWTException, InterruptedException   {
	   
		Folder folder;
		connectionStatus = "Initializing...";
		history = new StringBuilder();
//skipconfigfile=true;
		if (!skipconfigfile) read_ini();
		
		try {

			if (iconset=="2") {
				newmailicon = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("resources/newmailicon2.jpg"));
				nomailicon = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("resources/nomailicon2.jpg"));
				disconnectedicon = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("resources/disconnected2.jpg"));
			} else {
				newmailicon = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("resources/newmailicon.jpg"));
				nomailicon = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("resources/nomailicon.jpg"));
				disconnectedicon = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("resources/disconnected.jpg"));
			}
			
		} catch (Exception e) {
			System.out.println("couldnt load icon: " + e.getMessage()); 
			System.exit(0);
		}

		
		// build right click popup menu
		ActionListener actionListener = new PopupActionListener();
		PopupMenu popMenu= new PopupMenu();
		 
		MenuItem item1 = new MenuItem("Go to Mail");
		item1.addActionListener(actionListener);
		popMenu.add(item1);
		     
		MenuItem item2 = new MenuItem("About");
		item2.addActionListener(actionListener);
		popMenu.add(item2);

		MenuItem item5 = new MenuItem("History");
		item5.addActionListener(actionListener);
		popMenu.add(item5);

		MenuItem item4 = new MenuItem("Status");
		item4.addActionListener(actionListener);
		popMenu.add(item4);

//		MenuItem item6 = new MenuItem("Show Connection Messages");
//		item4.addActionListener(actionListener);
//		popMenu.add(item6);

		CheckboxMenuItem cbEnabled = new CheckboxMenuItem("Enabled");
		popMenu.add(cbEnabled);
		cbEnabled.setState(alertsEnabled);
			
		CheckboxMenuItem cbShowConAlerts = new CheckboxMenuItem("Show connection messages");
		popMenu.add(cbShowConAlerts);
		cbShowConAlerts.setState(showConAlerts);
		
		CheckboxMenuItem cbBalloonmode = new CheckboxMenuItem("Balloon style alerts");
		popMenu.add(cbBalloonmode);
		cbBalloonmode.setState(balloonmode );
		
		MenuItem item3 = new MenuItem("Exit");
		item3.addActionListener(actionListener);
		popMenu.add(item3);
		

		trayIcon = new TrayIcon(disconnectedicon, "Application Name", popMenu);
		trayIcon.setToolTip(emptytooltip);
		System.out.println("tray size is " + trayIcon.getSize() );
		SystemTray.getSystemTray().add(trayIcon);
		
		cbShowConAlerts.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
	          if (e.getStateChange() == ItemEvent.SELECTED){
							showConAlerts = true;
	          } else {
							showConAlerts = false;
	          }
	      }
	  	});
		
		cbBalloonmode.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
	        if (e.getStateChange() == ItemEvent.SELECTED){
	        	balloonmode = true;
	          } else {
	          	balloonmode = false;
	          }
	      }
	  	});
		
		cbEnabled.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
		        if (e.getStateChange() == ItemEvent.SELECTED){
					alertsEnabled = true;
		          } else {
		          	alertsEnabled = false;
		          }
	      }
	  	});
		
		// get login if needed
		if ( username.isEmpty() || cleartextpw.isEmpty() ) {
			
			JTextField loginField = new JTextField(10);
			  
			JPasswordField passwordField = new JPasswordField(10);
			loginField.setText(username);
			JPanel myPanel = new JPanel();
			myPanel.add(new JLabel("login:"));
			myPanel.add(loginField);
			myPanel.add(Box.createHorizontalStrut(15)); // a spacer
			myPanel.add(new JLabel("password:"));
			passwordField.setText(cleartextpw);
			myPanel.add(passwordField);
			   
			int result = JOptionPane.showConfirmDialog(null, myPanel, "Enter login info", JOptionPane.OK_CANCEL_OPTION);
			if (result == JOptionPane.CANCEL_OPTION) {
				System.out.println("cancelled...exiting");
				System.exit(0);      
			}
	  
			username = loginField.getText();
			cleartextpw = passwordField.getText();

			if (username.isEmpty() || cleartextpw.isEmpty() ) {
				alert("both username and password are required...exiting");
				Thread.sleep(60 * 1000);
				System.exit(0);      
			}
//System.exit(0);      
			
		} else {
			alert("logging in as " + username);
		}

		System.out.println("login is " + username);
		emptytooltip = username + " - No New Mail";
		 
		trayIcon.setToolTip(emptytooltip);


		// anonymous class created here...
		MouseListener ml = new MouseListener() {
			public void mouseClicked(MouseEvent e) {
				if (!mailempty) {
					if (++historycount > MAXHISTORY-1) history.delete(0, history.indexOf("\n") + 1); // keep history down to latest MAXHISTORY items 	
					history.append(trayIcon.getToolTip() + "\n" );
				}

				mailempty = true;
				gmonitor.trayIcon.setImage(nomailicon);
				trayIcon.setToolTip(emptytooltip);
			 }
			
			 public void mouseEntered(MouseEvent e) {}
			 public void mouseExited(MouseEvent e) {}
			 public void mousePressed(MouseEvent e) {}
			 public void mouseReleased(MouseEvent e) {}
		};
        		

		trayIcon.addMouseListener(ml);
      
      
      System.out.println("\nTesting gmonitor\n");

      // took out of try
      Properties props = System.getProperties();
      props.setProperty("mail.store.protocol", "imaps");
      props.setProperty("mail.imaps.host", "imap.gmail.com");
      props.setProperty("mail.imaps.port", "993");
      props.setProperty("mail.imaps.connectiontimeout", "5000");
      props.setProperty("mail.imaps.timeout", "5000");
      if (proxymode) {
	      props.setProperty("mail.imaps.socks.host", "localhost"); 
	      props.setProperty("mail.imaps.socks.port", "1080"); 
      }            
      
      // Get a Session object
      Session session = Session.getInstance(props, null);
      // session.setDebug(true);
      System.out.println( "Transport: "+props.getProperty("mail.transport.protocol"));
      System.out.println("Store: "+props.getProperty("mail.store.protocol"));
      System.out.println( "Host: "+props.getProperty("mail.imap.host"));
      System.out.println( "Authentication: "+props.getProperty("mail.imap.auth"));
      System.out.println( "Port imaps: "+props.getProperty("mail.imap2.port"));
      System.out.println( "Port imap: "+props.getProperty("mail.imap.port"));
      
      do {
         try {
            store = session.getStore("imaps");  // changed from imap to imaps
      
            store.connect(host, username, cleartextpw);
            if (showConAlerts) alert("logged in as " + username);
            
            connectionStatus = "logged in as " + username;
   
            // Open a Folder
            folder = store.getFolder(folderStr);
    
            if (folder == null || !folder.exists()) {
               System.out.println("Invalid folder");
               System.exit(1);
            }
    
            folder.open(Folder.READ_WRITE);
            System.out.println("opened folder...\n");
            
            //alert("Connected");
            gmonitor.trayIcon.setImage(nomailicon);
  
            // Add messageCountListener to listen for new messages (anonymous class created here...)
            folder.addMessageCountListener(new MessageCountAdapter() {
               public void messagesAdded(MessageCountEvent ev) {
                  Message[] msgs = ev.getMessages();
                  System.out.println("Gott " + msgs.length + " new messages");
                  trayIcon.setImage(newmailicon);
   
                  // Just dump out the new messages
                  for (int i = 0; i < msgs.length; i++) {
                     try {
                    	 System.out.println("at 12");    
                    	 
                 		boolean skip=false;
                 		
                 		String emailsender = msgs[i].getFrom()[0].toString();
                 		
                 		String subject = msgs[i].getSubject();

                 		
                 		// read email
                 		boolean displayContent = false;
/*                 		
                 		String contentType = msgs[i].getContentType();
               			if (contentType. contains("TEXT/PLAIN")  ) {
							//org.apache.commons.lang3.StringUtils.containsIgnoreCase("ABCDEFGHIJKLMNOP", "gHi");
							 Object content = msgs[i].getContent();
							 System.out.println("content: "+ content.toString());
						} else {
							System.out.println("nonmatching contenttype is " + contentType );
						}
  */               		
                 		for (String filter : filters) {
                 			if ( emailsender.contains(filter) || subject.contains(filter)) {
                 				skip=true;
                 			}
                 		}
                    	 
											if (!skip) {
												System.out.println("-----");
												System.out.println("Message " + msgs[i].getMessageNumber() );
												mailstring = emailsender;
	                        
	                        // trim extra crap off googlevoice subject
	                		if (mailstring.contains("@txt.voice.google.com>")) {
		      								mailstring = mailstring.split("<", 2)[0];
		                	    System.out.println("**** trimmed gv to "+ mailstring);
	                		} 
	                        
	                        String[] shortfrom = mailstring.split("<");
	                        //try {
	                           // Runtime.getRuntime().exec("alert mail from " + shortfrom[0]);
	                           if (alertsEnabled) alert("mail from " + shortfrom[0]);
	//                         } catch (IOException e) {  
	//                           e.printStackTrace();  
	//                         }
	                        if (mailstring.length() > 0) mailstring += ": ";
	                        mailstring += subject;
	                        //System.out.println("Subject: "+msgs[i].getSubject());
	                        //System.out.println("From: " + msgs[i].getFrom()[0].toString());
	                        System.out.println(mailstring);
	                        if (mailempty) {
	                           tooltip = "";
	                        } else {
	                           tooltip += "\n";
	                        }
	                        mailempty = false;
	                        tooltip += mailstring;
	                        trayIcon.setToolTip(tooltip);
                    	} else {
                    	  System.out.println("skipping email: from=" + emailsender + " subject=" + subject);
                    	}
                        
                     } catch (MessagingException mex) {
                        mex.printStackTrace();
                     } catch (Exception ex) {
                    	ex.printStackTrace();
                     }
                  }
               }
            });
    
   
            System.out.println("ok now to wait for mail...");
            // Check mail once in "freq" MILLIseconds
            boolean supportsIdle = false;
            try {
               if (folder instanceof IMAPFolder) {
                  IMAPFolder f = (IMAPFolder)folder;
                  System.out.println("going idle");
                  f.idle();
                  System.out.println("out of idle");
                  supportsIdle = true;
               }
            } catch (FolderClosedException fex) {
               System.out.println("folderclosed exception at 982, connected is " + store.isConnected());
               store.close();
               
               throw fex;
            } catch (MessagingException mex) {
               System.out.println("at 23");
               supportsIdle = false;
            } catch (Exception e) {
               System.out.println("unhandled exception at 1: " + e.getMessage());
            }
   
            for (;;) {
               if (supportsIdle && folder instanceof IMAPFolder) {
                  System.out.println("at 25");
                  IMAPFolder f = (IMAPFolder)folder;
                  System.out.println("going idle at 251\n.\n.\n.\n.");
                  f.idle();
                  System.out.println("out of IDLE, connected is " + store.isConnected() );

               } else {
                  System.out.println("THIS BLOCK NEVER RUNS...");
                  Thread.sleep(freq * 1000); // sleep for freq seconds
                  System.out.println("woke, connected is " + store.isConnected() + ", checking...");
                  // This is to force the IMAP server to send EXISTS notifications. 
                  //num_msgs = folder.getMessageCount();
               }
            }
    
         } catch (MessagingException ex) {
            if (ex.getMessage().contains("refused") ) {
            	connectionStatus = "CONNECTION ERROR: " + ex.getMessage();
            	if (showConAlerts) alert(connectionStatus);
            	
            } else if (ex.getMessage().contains("Invalid credentials") ) {
            	connectionStatus = "FATAL INVALID CREDENTIALS ERROR: " + ex.getMessage();
            	break;
            	
            } else {
            	connectionStatus = "UNDEFINED ERROR: " + ex.getMessage();
            	if (showConAlerts) alert(connectionStatus);
            }
            
            gmonitor.trayIcon.setImage(disconnectedicon);
          	Thread.sleep(60 * 1000);

   
         } catch (IllegalStateException ex) {
        	 // this occurs a lot
         } catch (Exception ex) {
						connectionStatus = "unhandled exception: " + ex.getMessage();
						System.out.println(ex.getStackTrace());
						System.out.println("exception type=" + ex.toString());
						System.out.println(connectionStatus);
						if (showConAlerts) alert(connectionStatus);
         }
      
      } while (true);
      
      //javax.swing.JOptionPane.showMessageDialog(null, errorMessage );
      connectionStatus = "exiting";

      System.out.println("exiting" );
      System.exit(0);
   }

   private static void read_ini()  {
	   HashMap<String, String> param = new HashMap<String, String>();

	   try {
			
			BufferedReader br = new BufferedReader(new FileReader(CONFIGFILE));
			String line;
			while ((line = br.readLine()) != null) {

				if (line.charAt(0) == '#') continue;

			    String[] tokens = line.split("=", 2);

				param.put(tokens[0],   tokens.length == 2 ? tokens[1] : "" );
				System.out.println("toked "+ tokens[0]);				
			}
			
			br.close();
			
			for ( String key : param.keySet() ) {
				System.out.println(key + "=" + param.get(key) );
			}
			
			if ( param.containsKey("filter") ) {
				
				filters = Arrays.asList(param.get("filter").split(",") );
				
				for (String str : filters) {
					System.out.println("filterstr=" + str);
				}
			}
			
			if ( param.containsKey("encryptedpassword") ) {
				cleartextpw = decodepw(param.get("encryptedpassword"));
		        //System.out.println("***** decrypted to " + cleartextpw);
			}	

			if ( param.containsKey("username") ) {
				username = param.get("username");
			}

			if ( param.containsKey("iconset") ) {
				iconset = param.get("iconset");
			}
			
			if ( param.containsKey("proxy") ) {
				proxymode = (param.get("proxy").charAt(0) == '0' ? false : true );
				System.out.println("proxy=" + proxymode );
			}

			if ( param.containsKey("balloonmode") ) {
				balloonmode = (param.get("balloonmode").charAt(0) == '0' ? false : true );
				System.out.println("balloonmode=" + balloonmode );
			}

			if ( param.containsKey("cleartextpassword") ) {
				
				// encrypt it and display alert
				System.out.println("now to encrypt " + encodepw(param.get("cleartextpassword")) ) ;
		    	String encodedpw = encodepw(param.get("cleartextpassword"));
				System.out.println("****** encoded to " + encodedpw);
				
				cleartextpw = param.get("cleartextpassword");

			    JTextField encryptedpwfield = new JTextField(10);
			    encryptedpwfield.setText(encodedpw);
			    JPanel passwordalert = new JPanel();
			    passwordalert.add(new JLabel("the encrypted password is "));
			    passwordalert.add(encryptedpwfield);
			    JOptionPane.showConfirmDialog(null, passwordalert, "Encrypted password", JOptionPane.PLAIN_MESSAGE);
				
			}
				
		} catch (Exception e) {
			System.out.println("Error opening ini file, so ignoring ini");
		}		
				
   }		
			
   private static String generateKey() throws Exception {
	  KeyGenerator kgen = KeyGenerator.getInstance(ALGORITHM);
	  kgen.init(56);
	  SecretKey key = kgen.generateKey();
	  byte[] raw = key.getEncoded();
	  String b64key = DatatypeConverter.printBase64Binary(raw);
	  return b64key;
   }
   
   private static String decodepw(String encodedpw) throws Exception {
		byte[] decoded = DatatypeConverter.parseBase64Binary(encodedpw);
		
		System.out.println("using keyyy " + b64key );
		
		byte[] decodedkey =  DatatypeConverter.parseBase64Binary(b64key);

		SecretKeySpec keySpec = new SecretKeySpec(decodedkey, ALGORITHM );
		Cipher cipher = Cipher.getInstance(ALGORITHM);
	    cipher.init(Cipher.DECRYPT_MODE, keySpec);
	    byte[] newPlainText = cipher.doFinal(decoded ); // encrypted
	    
	    
	    return new String(newPlainText, CHARSET); 
   }
   
   public static void clearMailflag()  {
System.out.println("clearing flag");	   
	   gmonitor.trayIcon.setImage(gmonitor.nomailicon);
   }

   
   private static String encodepw(String cleartextpw) throws Exception {

	   byte[] decodedkey =  DatatypeConverter.parseBase64Binary(b64key);
	   // UNCOMMENT TO CHANGE THE KEY:  System.out.println("new key: " + generateKey() );
       
       SecretKeySpec keySpec = new SecretKeySpec(decodedkey, ALGORITHM);
       Cipher cipher = Cipher.getInstance(ALGORITHM);
       cipher.init(Cipher.ENCRYPT_MODE, keySpec);
       byte[] encrypted = cipher.doFinal(cleartextpw.getBytes(CHARSET));

       return DatatypeConverter.printBase64Binary( encrypted );
   	
   }

   
   // alert code from http://harryjoy.com/2011/07/01/create-new-message-notification-pop-up-in-java/
   public static void alert (String message) {

	   /*      
	      AePlayWave aw = new AePlayWave( "notify2.wav" );  //  resources/notify2.wav   C:\\WINDOWS\\Media\\tada.wav
	      aw.start(); 
	*/
	   
	   System.out.println(message);
	   
	   if (balloonmode) { // balloon mode

		   trayIcon.displayMessage("", message, TrayIcon.MessageType.INFO);

	   } else {
		   
	      final JFrame frame = new JFrame();
	      frame.setLocation(700, 450);  // 700,450 = middle   1550 850 = lower right
	      frame.setSize(300,125);
	      frame.setUndecorated(true);
	      frame.setLayout(new GridBagLayout());
	      frame.setAlwaysOnTop(true);
	      GridBagConstraints constraints = new GridBagConstraints();
	      constraints.gridx = 0;
	      constraints.gridy = 0;
	      constraints.weightx = 1.0f;
	      constraints.weighty = 1.0f;
	      constraints.insets = new Insets(5, 5, 5, 5);
	      constraints.fill = GridBagConstraints.BOTH;
	      JLabel messageLabel = new JLabel("<HtMl>"+message);
	      frame.add(messageLabel, constraints);
	      frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	      frame.setVisible(true);
	 
	      // anonymous class created here...
	      new Thread(){
	         @Override
	         public void run() {
	              try {
	                     Thread.sleep(1000); // time after which pop up will be disappeared.
	                     frame.dispose();
	              } catch (InterruptedException e) {
	                     e.printStackTrace();
	              }
	         };
	      }.start();
	   }

     }
   
   class PopupActionListener implements ActionListener {
      public void actionPerformed(ActionEvent actionEvent) {
        
        String cmd = actionEvent.getActionCommand();
        if (cmd == "Exit") {
	       	try {
      		
	            if (gmonitor.store.isConnected()) gmonitor.store.close();
	            
	        } catch (MessagingException e) {
	            System.out.println("exception during close: " + e.getMessage()  );
	        }
	        alert("exiting now");
	        System.exit(0);
	          
        } else if (cmd == "History") {
        	javax.swing.JOptionPane.showMessageDialog(null, history);
        	
        } else if (cmd == "Status") {
        	javax.swing.JOptionPane.showMessageDialog(null, connectionStatus);
        	
        } else if (cmd == "Go To Mail") {
           gmonitor.trayIcon.setImage(gmonitor.nomailicon);
           try { Runtime.getRuntime().exec("focus.exe Chrome");  } catch (Exception e) {  }
           
        } else if (cmd == "About") {
          javax.swing.JOptionPane.showMessageDialog(null,"GMonitor " + BUILDDATE);
          gmonitor.trayIcon.setImage(gmonitor.nomailicon);
        }  
      }
    }
}

// scan for CHECKFILE and when found, delete it and clear mail indicator 
class clearIconWatchdog extends Thread {
    public clearIconWatchdog() {
    	super();
    }
    public void run() {
	    File fname = new File(gmonitor.CHECKFILE);

	    while (true) {
		    while( !fname.exists() ){
		    	System.out.println("doesnt exist...sleeping");
			    try { Thread.sleep(4000); } catch (InterruptedException e) {}		    	
		    }
		    gmonitor.clearMailflag();
		    fname.delete();
	    }
    }
}
