package com.github.kevinmussi.itunesrp.core;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.kevinmussi.itunesrp.data.ScriptCommand;
import com.github.kevinmussi.itunesrp.observer.Observable;
import com.github.kevinmussi.itunesrp.observer.Observer;

public class AppleScriptHelper
		extends Observable<String> implements Observer<ScriptCommand> {
	
	public static final String TRACK_RECORD_SEPARATOR = ";;";
	
	private final Logger logger = Logger.getLogger(getClass().getSimpleName() + "Logger");
	
	private final ProcessBuilder builder;
	private volatile Process process;
	
	public AppleScriptHelper(String script) {
		this.builder = new ProcessBuilder("osascript", "-e", script);
		// The script logs its messages to stderr, so we need to redirect it to the stdout
		this.builder.redirectErrorStream(true);
		this.process = null;
	}
	
	@Override
	public void update(ScriptCommand message) {
		switch(message) {
			case EXECUTE:
				if(!(process != null && process.isAlive())) {
					new Thread(this::executeScript);
				}
				break;
			case KILL:
				stopScript();
				break;
		}
	}
	
	public void executeScript() {
		if(process != null && process.isAlive()) {
			return;
		}
		try {
			process = builder.start();
			logger.log(Level.INFO, () -> LocalDateTime.now().toString() + " The script started execution.");
			Scanner scanner = new Scanner(process.getInputStream());
			scanner.useDelimiter("\n");
			while(process.isAlive()) {
				if(scanner.hasNext()) {
					notifyObservers(scanner.next());
				}
			}
			scanner.close();
		} catch (IOException e) {
			logger.log(Level.SEVERE, LocalDateTime.now().toString() + " The script did not execute correctly", e);
		}
	}
	
	public void stopScript() {
		if(process != null && process.isAlive()) {
			process.destroy();
			logger.log(Level.INFO, () -> LocalDateTime.now().toString() + " The script stopped execution.");
		}
		process = null;
	}
	
}