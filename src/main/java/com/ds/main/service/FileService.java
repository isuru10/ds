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

/**
 * @desc file service to handle the serving files
 */
@Service
public class FileService
{
	Random randomNum = new Random();
	String files[] = new String[20];
	String[] servingFiles;

	private static String staticFileLocation = "/src/main/resources/static/created_files/";

	public FileService() throws IOException
	{
		File file = ResourceUtils.getFile( "classpath:static/File_Names.txt" );
		BufferedReader br = new BufferedReader( new FileReader( file ) );

		String st;
		int counter = 0;
		while ( ( st = br.readLine() ) != null )
		{
			files[counter] = st;
			counter++;
		}
		br.close();
		setServingFiles();
	}

	/**
	 * @param name
	 * @return
	 * @desc get a single file
	 */
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

	/**
	 * @return
	 * @throws IOException
	 * @desc get all files
	 */
	public String[] getAll() throws IOException
	{
		return files;
	}

	/**
	 * @return
	 * @desc get all serving files
	 */
	public String[] getAllServingFiles()
	{
		return servingFiles;
	}

	/**
	 * @desc set the serving files
	 */
	public void setServingFiles()
	{
		int rand = 3 + randomNum.nextInt( 6 - 3 );
		servingFiles = new String[rand];
		for ( int i = 0; i < rand; i++ )
		{
			int index = randomNum.nextInt( 20 );
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

			String writingStr = new String( chars );

			MessageDigest digest = MessageDigest.getInstance( "SHA-256" );
			byte[] hash = digest.digest( writingStr.getBytes( StandardCharsets.UTF_8 ) );
			String encoded = Base64.getEncoder().encodeToString( hash );

			System.out.println(
					"The file is: " + name + "\nwith size:" + fileSize / ( 1024 * 1024 ) + "Mb\nHash:" + encoded );

			String workingDirectory = System.getProperty( "user.dir" );
			String target = workingDirectory + staticFileLocation + name + ".txt";
			writer = new BufferedWriter( new FileWriter( target ) );
			writer.write( writingStr );
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
