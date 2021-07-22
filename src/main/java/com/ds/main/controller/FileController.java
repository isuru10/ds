package com.ds.main.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import com.ds.main.service.FileService;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@RestController
@RequestMapping("/files")
public class FileController
{

	@Autowired
	private FileService fileService;

	@RequestMapping("/file")
	public HashMap<String, String> fetchOneFile( @RequestParam(value = "name") String name )
	{
		HashMap<String, String> map = new HashMap<>();
		map.put( "name", fileService.getFile( name ) );
		return map;
	}

	@RequestMapping("/all")
	public String[] fetchAllFiles() throws IOException
	{
		return fileService.getAllServingFiles();
	}

	@RequestMapping(path = "/downloadFile", method = RequestMethod.GET)
	public ResponseEntity<Resource> downloadFile( @RequestParam(value = "name") String name )
			throws IOException, NoSuchAlgorithmException
	{

		String[] servingFiles = fileService.getAllServingFiles();

		boolean isFileAvailable = false;
		for ( String i : servingFiles )
		{
			if ( i.equalsIgnoreCase( name ) )
			{
				isFileAvailable = true;
				break;
			}
		}

		if ( isFileAvailable )
		{

			HttpHeaders headers = new HttpHeaders();
			String headerValue = "attachment; filename=" + name + ".txt";
			headers.add( HttpHeaders.CONTENT_DISPOSITION, headerValue );

			String target = fileService.createRandomFile( name );

			File file = ResourceUtils.getFile( target );
			Path path = Paths.get( file.getAbsolutePath() );
			ByteArrayResource resource = new ByteArrayResource( Files.readAllBytes( path ) );

			//send created file
			return ResponseEntity.ok()
					.headers( headers )
					.contentLength( file.length() )
					.contentType( MediaType.parseMediaType( "application/octet-stream" ) )
					.body( resource );
		}
		else
		{
			System.out.println( "File does not exists!" );
		}
		return null;
	}
}
