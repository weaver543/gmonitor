// package com.tutorialspoint;
// using TLS auth from http://www.mkyong.com/java/javamail-api-sending-email-via-gmail-smtp-example/
// http://www.tutorialspoint.com/javamail_api/javamail_api_send_email_with_attachment.htm

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Properties;
import java.util.StringTokenizer;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.xml.bind.DatatypeConverter;

public class jsend {

	static String from = System.getenv("FROM");
	static String to = System.getenv("TO");
	static String encryptedpassword = System.getenv("ENCRYPTEDPASSWORD");
	static String login = System.getenv("LOGIN");
	static String password = System.getenv("PASSWORD");
	static String proxyport = System.getenv("PROXYPORT");
	
	static final String host = "smtp.gmail.com";
	static final String ALGORITHM = "Blowfish";
	static final String b64key = "itZbdUHhGg==";
	static final String CHARSET = "UTF-8";
	static String messagetext; 
	static String filename = "";
	
	public static void main(String[] args) {
		
		if (args.length > 0) {
			
			messagetext = args[0].replaceFirst("^ ", "");  // trim leading space from e script
			
			if (messagetext.startsWith("./")) {
				filename = messagetext.split(" ", 2)[0];
				messagetext = messagetext.replaceFirst(filename, "");
				filename = filename.replaceFirst("\\./", "");
			}

		} else {
			System.out.println("nothing to send");
			System.exit(0);
		}

		if (password != null) System.out.println("encoded password is " + encodepw(password) );
		if (encryptedpassword != null) password = decodepw(encryptedpassword);

		// at this point if environment vars didnt populate enough then read_ini
		if ((from==null) || (to==null) || (login==null) || (password==null)  ) {
	  	read_ini();
	  }

		if ( (login == null || password == null) || (login.isEmpty() || password.isEmpty()) ) {
			System.out.println("login or password is missing; aborting");
			System.exit(0);
		}

		if ( (from == null || to == null) || (from.isEmpty() || to.isEmpty())  ) {
			System.out.println("from or to address or proxyport is missing; aborting");
			System.exit(0);
		}
		
		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", host);
		props.put("mail.smtp.port", "587");

		
		
		if (proxyport != null && !proxyport.isEmpty()) {
			props.put("mail.smtp.socks.host", "localhost"); 
			props.put("mail.smtp.socks.port", Integer.valueOf(proxyport)  );
    }
    
    
    // Get the Session object.
    Session session = Session.getInstance(props,
       new javax.mail.Authenticator() {
          protected PasswordAuthentication getPasswordAuthentication() {
             return new PasswordAuthentication(login, password);
          }
       });
    try {
    	
			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(from));
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
		
			if (filename.length() == 0) {
				message.setSubject("notifications");
				message.setText(messagetext);
		
			} else {
				System.out.println("sending attachment: " + filename);				
		  	
				message.setSubject("sending " + filename);
				BodyPart messageBodyPart = new MimeBodyPart();
				messageBodyPart.setText(messagetext);
				
				Multipart multipart = new MimeMultipart();
				multipart.addBodyPart(messageBodyPart);
				 
				// Part two is OPTIONAL attachment
				messageBodyPart = new MimeBodyPart();
				DataSource source = new FileDataSource( System.getProperty("user.dir") + "/" + filename);
				messageBodyPart.setDataHandler(new DataHandler(source));
				messageBodyPart.setFileName(filename);
				multipart.addBodyPart(messageBodyPart);
				
				message.setContent(multipart);
			}
			
			Transport.send(message);
		
			System.out.println("Sent message successfully!");

    } catch (MessagingException e) {
       throw new RuntimeException(e);
    }
	}
   
 	private static void read_ini() {
 		
		HashMap<String, String> param = new HashMap<String, String>();
   
		
		File classpathFile = findFileOnClassPath("jsend.ini");

//		    String classpath = System.getProperty("java.class.path");
//			BufferedReader br = new BufferedReader(new FileReader(classpath + "/jsend.ini"));
		try {		
		  
			BufferedReader br = new BufferedReader(new FileReader(classpathFile.getAbsolutePath()));
			
			String line;
			  
			while ((line = br.readLine()) != null) {
			
				String[] tokens = line.split("=", 2);
				
				param.put(tokens[0],   tokens.length == 2 ? tokens[1] : "" );
			}
			  
			br.close();
			  
//			for ( String key : param.keySet() ) {
//				System.out.println(key + "=" + param.get(key) );
//			}

			if ( param.containsKey("login") && login==null ) login = param.get("login");
			if ( param.containsKey("from") && from==null ) from = param.get("from");
			if ( param.containsKey("to") && to==null) to = param.get("to");
			if ( param.containsKey("proxyport") && proxyport==null) proxyport = param.get("proxyport");
			if ( param.containsKey("encryptedpassword") && password==null) password = decodepw(param.get("encryptedpassword"));

			if ( param.containsKey("password") && password==null) {
				password = param.get("password");
				System.out.println("Encrypted password: " + encodepw(password) );
			}
			
		} catch (Exception e) {
				e.printStackTrace();
		}

 	}
	
	private static String decodepw(String encodedpw)  {
		byte[] decoded = DatatypeConverter.parseBase64Binary(encodedpw);
		byte[] decodedkey =  DatatypeConverter.parseBase64Binary(b64key);
		byte[] newPlainText = null;
		String decodedpassword = null;
	
		try {
			SecretKeySpec keySpec = new SecretKeySpec(decodedkey, ALGORITHM );
			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, keySpec);
			newPlainText = cipher.doFinal(decoded ); // encrypted
			decodedpassword = new String(newPlainText, CHARSET); 
		} catch (Exception e) {
			e.printStackTrace();
		}
		return decodedpassword; 
	}
	 
	private static String encodepw(String cleartextpw) {
		byte[] encrypted = null;

		try {

			byte[] decodedkey =  DatatypeConverter.parseBase64Binary(b64key);
		   // UNCOMMENT TO CHANGE THE KEY:  System.out.println("new key: " + generateKey() );
		     
			SecretKeySpec keySpec = new SecretKeySpec(decodedkey, ALGORITHM);
			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, keySpec);
			encrypted = cipher.doFinal(cleartextpw.getBytes(CHARSET));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return DatatypeConverter.printBase64Binary( encrypted );
	}
	
	private static File findFileOnClassPath(final String fileName) {
	   
		final String classpath = System.getProperty("java.class.path");
		final String pathSeparator = System.getProperty("path.separator");
		final StringTokenizer tokenizer = new StringTokenizer(classpath, pathSeparator);
	
		while (tokenizer.hasMoreTokens()) {
	
	     final String pathElement = tokenizer.nextToken();
	     final File directoryOrJar = new File(pathElement);
	     final File absoluteDirectoryOrJar = directoryOrJar.getAbsoluteFile();
	
	     if (absoluteDirectoryOrJar.isFile()) {
	
	       final File target = new File(absoluteDirectoryOrJar.getParent(), fileName);
	
	       if (target.exists()) {
	         return target;
	       }
	
	     } else {
	
	       final File target = new File(directoryOrJar, fileName);
	
	       if (target.exists()) {
	         return target;
	       }
	     }
	   }
	
	   return null;
	
	 }	
   
}