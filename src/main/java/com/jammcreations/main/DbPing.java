package com.jammcreations.main;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class DbPing
{
    public static void main( final String[] args )
    {
        final String jdbcUrl = getEnv( "DB_PING_JDBC_URL" );
        final String gmailAddr = getEnv( "DB_PING_GMAIL_ADDR" );
        final String gmailPassword = getEnv( "DB_PING_GMAIL_PASSOWRD" );
        final String to = getEnv( "DB_PING_TO" );

        final File lockFile = new File( "/tmp/DB_PING.lock" );
        if ( lockFile.exists() )
        {
            System.out.println( "Lockfile found.  Exiting." );
            return;
        }

        try
        {
            pingDb( jdbcUrl );
        }
        catch ( final SQLException e )
        {
            e.printStackTrace();
            try
            {
                lockFile.createNewFile();
            }
            catch ( final Exception ignore )
            {
            }
            try
            {
                send( gmailAddr, gmailPassword, to, "DB PING FAILED", e.getMessage() );
            }
            catch ( final MessagingException ex )
            {
                ex.printStackTrace();
            }
        }

    }

    private static String getEnv( final String key )
    {
        final String env = System.getenv( key );
        if ( env == null )
        {
            throw new RuntimeException( "Environment variable '" + key + "' is not set." );
        }

        return env;
    }

    private static void pingDb( final String jdbcUrl ) throws SQLException
    {
        final Connection conn = DriverManager.getConnection( jdbcUrl );
        conn.close();
    }

    private static void send( final String from, final String password, final String to,
                              final String sub, final String msg ) throws MessagingException
    {
        // Get properties object
        final Properties props = new Properties();
        props.put( "mail.smtp.host", "smtp.gmail.com" );
        props.put( "mail.smtp.socketFactory.port", "465" );
        props.put( "mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory" );
        props.put( "mail.smtp.auth", "true" );
        props.put( "mail.smtp.port", "465" );
        // get Session
        final Session session = Session.getDefaultInstance( props, new javax.mail.Authenticator()
        {
            @Override
            protected PasswordAuthentication getPasswordAuthentication()
            {
                return new PasswordAuthentication( from, password );
            }
        } );
        // compose message
        final MimeMessage message = new MimeMessage( session );
        final String[] tos = to.split( "," );
        for ( final String toAddr : tos )
        {
            message.addRecipient( Message.RecipientType.TO, new InternetAddress( toAddr ) );
        }
        message.setSubject( sub );
        message.setText( msg );
        // send message
        Transport.send( message );
        System.out.println( "message sent successfully" );
    }
}
