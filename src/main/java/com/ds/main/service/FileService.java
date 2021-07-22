package com.ds.main.service;

import com.ds.main.SpringBootRestApplication;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;

/***  file service to handle the serving files */
@Service
public class FileService
{
	Random rNum = new Random();
	String files[] = new String[20];
	String[] servingFiles;

	private static String staticFileLocation = "/src/main/resources/static/created_files/";

	public FileService() throws IOException
	{
		File filename = ResourceUtils.getFile( "classpath:static/File_Names.txt" );
		BufferedReader br = new BufferedReader( new FileReader( filename ) );

		String fname;
		int counter = 0;
		while ( ( fname = br.readLine() ) != null )
		{
			files[counter] = fname;
			counter++;
		}
		br.close();
		setServingFiles();
	}

	/** get all files */
	public String[] getAll() throws IOException
	{
		return files;
	}

	/*** get a single file */
	public String getFile( String name )
	{
		for ( int i = 0; i < servingFiles.length; i++ )
		{
			if ( servingFiles[i].equals( name ) )
			{
				return servingFiles[i];
			}
		}
		return null;
	}

	/*** get all serving files */
	public String[] getAllServingFiles()
	{
		return servingFiles;
	}

	/***  set the serving files */
	public void setServingFiles()
	{
		int rand = 3 + rNum.nextInt( 6 - 3 );
		servingFiles = new String[rand];
		for ( int i = 0; i < rand; i++ )
		{
			int index = rNum.nextInt( 20 );
			servingFiles[i] = files[index];
		}
		SpringBootRestApplication.servingFiles = this.servingFiles;
	}

	public String createRandomFile( String name )
	{
		BufferedWriter writer = null;

		try
		{
			Random rand = new Random();
			int fileSize = ( 2 + rand.nextInt( 8 ) ) * 1024 * 1024;
			char[] chars = new char[fileSize];
			Arrays.fill( chars, 'z' );

			String wStr = new String( chars );

			MessageDigest digest = MessageDigest.getInstance( "SHA-256" );
			byte[] hash = digest.digest( wStr.getBytes( StandardCharsets.UTF_8 ) );
			String encoded = Base64.getEncoder().encodeToString( hash );

			System.out.println(
					"The file is: " + name + "\nwith size:" + fileSize / ( 1024 * 1024 ) + "Mb\nHash:" + encoded );

			String workingDir = System.getProperty( "user.dir" );
			String target = workingDir + staticFileLocation + name + ".txt";
			writer = new BufferedWriter( new FileWriter( target ) );
			writer.write( wStr );
			return target;
		}
		catch ( NoSuchAlgorithmException | IOException e )
		{
			e.printStackTrace();
		}
		finally
		{
			if ( writer != null )
			{
				try
				{
					writer.close();
				}
				catch ( IOException e )
				{
					e.printStackTrace();
				}
			}
		}
		return null;
	}
}
