/*
 Copyright 2014-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.

 Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with the License. A copy of the License is located at

 http://aws.amazon.com/apache2.0/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package sky;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

import javax.json.*;

import config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SessionEndedRequest;
import com.amazon.speech.speechlet.SessionStartedRequest;
import com.amazon.speech.speechlet.Speechlet;
import com.amazon.speech.speechlet.SpeechletException;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;

import static java.lang.System.*;

/**
 * This sample shows how to create a simple speechlet for handling speechlet requests.
 */
public class SkySpeechlet implements Speechlet {
	private static final Logger log = LoggerFactory.getLogger(SkySpeechlet.class);
	private String SKY_PLAY_URL = "http://%s:49153/%sSkyPlay";
	private String SKY_BROWSE_URL = "http://%s:49153/%sSkyBrowse";

	public void setSkyPlayUrl(String ip, String uuid) {
		SKY_PLAY_URL = String.format(SKY_PLAY_URL, ip, uuid);
		log.info("SkyPlay Url: " + SKY_PLAY_URL);
	}

	public void setSkyBrowseUrl(String ip, String uuid) {
		SKY_BROWSE_URL = String.format(SKY_BROWSE_URL, ip, uuid);
		log.info("SkyBrowse Url: " + SKY_BROWSE_URL);
	}

	@Override
	public void onSessionStarted(final SessionStartedRequest request, final Session session)
			throws SpeechletException {
		log.info("onSessionStarted requestId={}, sessionId={}", request.getRequestId(),
				session.getSessionId());
		// any initialization logic goes here
	}

	@Override
	public SpeechletResponse onLaunch(final LaunchRequest request, final Session session)
			throws SpeechletException {
		log.info("onLaunch requestId={}, sessionId={}", request.getRequestId(),
				session.getSessionId());
		return getWelcomeResponse();
	}

	@Override
	public SpeechletResponse onIntent(final IntentRequest request, final Session session)
			throws SpeechletException {
		log.info("onIntent requestId={}, sessionId={}", request.getRequestId(),
				session.getSessionId());

		Intent intent = request.getIntent();
		String intentName = (intent != null) ? intent.getName() : null;

		if ("PlayRecordingIntent".equals(intentName)) {
			return getPlayRecordingResponse(intent);
		} else if ("FastForwardIntent".equals(intentName)) {
			return getFastForwardResponse(intent);
		} else if ("RewindIntent".equals(intentName)) {
			return getRewindResponse(intent);
		} else if ("PauseIntent".equals(intentName)) {
			return getPauseResponse();
		} else if ("StopIntent".equals(intentName)) {
			return getStopResponse();
		} else if ("AMAZON.HelpIntent".equals(intentName)) {
			return getHelpResponse();
		} else {
			throw new SpeechletException("Invalid Intent");
		}
	}

	@Override
	public void onSessionEnded(final SessionEndedRequest request, final Session session)
			throws SpeechletException {
		log.info("onSessionEnded requestId={}, sessionId={}", request.getRequestId(),
				session.getSessionId());
		// any cleanup logic goes here
	}

	/**
	 * Creates and returns a {@code SpeechletResponse} with a welcome message.
	 *
	 * @return SpeechletResponse spoken and visual response for the given intent
	 */
	private SpeechletResponse getWelcomeResponse() {
		String speechText = "This is Sky, how can I help you";

		// create the Simple card content
		SimpleCard card = new SimpleCard();
		card.setTitle("Sky");
		card.setContent(speechText);

		// create the plain text output
		PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
		speech.setText(speechText);

		// create reprompt
		Reprompt reprompt = new Reprompt();
		reprompt.setOutputSpeech(speech);

		return SpeechletResponse.newAskResponse(speech, reprompt, card);
	}

	/**
	 * Creates a {@code SpeechletResponse} for the PlayRecording intent.
	 *
	 * @return SpeechletResponse spoken and visual response for the given intent
	 */
	private SpeechletResponse getPlayRecordingResponse(Intent intent) {
		String speechText;

		// create the card
		SimpleCard card = new SimpleCard();
		card.setTitle("Play Recording");

		// create the plain text output
		PlainTextOutputSpeech speech = new PlainTextOutputSpeech();

		try {
			Slot programSlot = intent.getSlot("ProgramName");
			if (programSlot != null) {
				HashMap<String,Recording> recordings = new HashMap<>();
				getRecordings(recordings, 0);

				String programName = programSlot.getValue().toLowerCase();
				Recording match = recordings.get(programName);

				// if no exact match found, check for partial exact match
				if (match == null) {
					for (Recording recording : recordings.values()) {
						if (recording.title.contains(programName)) {
							out.println("Partial match");
							match = recording;
							break;
						}
					}
				}

				// if still no match found, check for percentage (say 75%) match
				if (match == null) {
					for (Recording recording : recordings.values()) {
						double similarity = similarity(recording.title, programName);
						if (similarity >= 0.75) {
							out.println(String.valueOf(similarity) + " match");
							match = recording;
							break;
						}
					}
				}

				if (match != null) {
					playRecording(match);

					//speechText = "Hurray it worked. I am playing " + programName + " for you Helen. Love from Bob.";
                    speechText = "Enjoy watching " + programName + ". Love, from Sky.";
				}
				else {
					speechText = "Found " + recordings.size() + " recordings, but '" + programName + "' is not one of them";
				}
			}
			else {
				speechText = "No recording name was heard";
			}

			setFeedback(speechText, speech, card);
		}
		catch (Exception e) {
			reportException(e, speech, card);
			speech.setText("Failed to play recording");
		}

		return SpeechletResponse.newTellResponse(speech, card);
	}

	private SpeechletResponse getFastForwardResponse(Intent intent) {
		String speechText;

		// create the card
		SimpleCard card = new SimpleCard();
		card.setTitle("Fast Forward");

		// create the plain text output
		PlainTextOutputSpeech speech = new PlainTextOutputSpeech();

		try {
			Slot speedSlot = intent.getSlot("Speed");
			if (speedSlot != null) {
				String speed = speedSlot.getValue();
				changeRecordingSpeed(speed);

				speechText = "Fast forward at " + speed + " times";
			}
			else {
				speechText = "No speed was heard";
			}

			setFeedback(speechText, speech, card);
		}
		catch (Exception e) {
			reportException(e, speech, card);
			speech.setText("Failed to fast forward");
		}

		return SpeechletResponse.newTellResponse(speech, card);
	}

	private SpeechletResponse getRewindResponse(Intent intent) {
		String speechText;

		// create the card
		SimpleCard card = new SimpleCard();
		card.setTitle("Rewind");

		// create the plain text output
		PlainTextOutputSpeech speech = new PlainTextOutputSpeech();

		try {
			Slot speedSlot = intent.getSlot("Speed");
			if (speedSlot != null) {
				String speed = speedSlot.getValue();
				changeRecordingSpeed("-" + speed);

				speechText = "Rewind at " + speed + " times";
			}
			else {
				speechText = "No speed was heard";
			}

			setFeedback(speechText, speech, card);
		}
		catch (Exception e) {
			reportException(e, speech, card);
			speech.setText("Failed to rewind");
		}

		return SpeechletResponse.newTellResponse(speech, card);
	}

	private SpeechletResponse getPauseResponse() {
		String speechText;

		// create the card
		SimpleCard card = new SimpleCard();
		card.setTitle("Pause");

		// create the plain text output
		PlainTextOutputSpeech speech = new PlainTextOutputSpeech();

		try {
			pauseRecording();
			speechText = "Paused playback";

			setFeedback(speechText, speech, card);
		}
		catch (Exception e) {
			reportException(e, speech, card);
			speech.setText("Failed to pause playback");
		}

		return SpeechletResponse.newTellResponse(speech, card);
	}

	private SpeechletResponse getStopResponse() {
		String speechText;

		// create the card
		SimpleCard card = new SimpleCard();
		card.setTitle("Stop");

		// create the plain text output
		PlainTextOutputSpeech speech = new PlainTextOutputSpeech();

		try {
			stopRecording();
			speechText = "Stopped playback";

			setFeedback(speechText, speech, card);
		}
		catch (Exception e) {
			reportException(e, speech, card);
			speech.setText("Failed to stop playback");
		}

		return SpeechletResponse.newTellResponse(speech, card);
	}

	private void setFeedback(String speechText, PlainTextOutputSpeech speech, SimpleCard card) {
		speech.setText(speechText);
		card.setContent(speechText);
	}

	private void reportException(Exception e, PlainTextOutputSpeech speech, SimpleCard card) {
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		String exceptionAsString = sw.toString();
		err.println(exceptionAsString);

		//speech.setText(exceptionAsString);
		card.setContent(exceptionAsString);
	}

	private HashMap<String,Recording> getRecordings(HashMap<String,Recording> recordings, Integer startingIndex)
			throws Exception {
		String body = String.format(BROWSE_BODY, startingIndex);
		String response = sendToSky(body, "Browse", false);

		boolean more = extractRecordingsFromResponse(recordings, response, startingIndex);
		if (more)
			return getRecordings(recordings, startingIndex + 25);

		err.println("Found " + recordings.size() + " recordings");
		for (Recording recording : recordings.values())
			err.println("Title, " + recording.title + ", Date: " + recording.broadcastDate + ", Id: " + recording.id);
		return recordings;
	}

	private boolean extractRecordingsFromResponse(HashMap<String,Recording> recordings, String response, int startingIndex)
			throws Exception {
		int returnCount = 0, totalCount = 0;

		// parse the returned xml to create the recording objects
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();

		Document document = builder.parse(new InputSource(new StringReader(response)));
		//List<Employee> employees = new ArrayList<Employee>();
		NodeList nodeList = document.getDocumentElement().getElementsByTagName("u:BrowseResponse").item(0).getChildNodes();

		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);

			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element elem = (Element) node;

				if (elem.getTagName().equals("Result")) {
					Document document2 = builder.parse(new InputSource(new StringReader(elem.getTextContent())));
					NodeList items = document2.getDocumentElement().getElementsByTagName("item");

					for (int j = 0; j < items.getLength(); j++) {
						Element item = (Element) items.item(j);

						String title = item.getElementsByTagName("dc:title").item(0).getTextContent().trim().toLowerCase();
						//System.err.println("Rec title, " + title);

						int channelNumber = Integer.parseInt(item.getElementsByTagName("upnp:channelNr").item(0).getTextContent());
						if (channelNumber == 65535) {
							err.println("Ignore '" + title + "', has not been recorded");
							continue;
						}

						int recStatus = Integer.parseInt(item.getElementsByTagName("vx:X_recStatus").item(0).getTextContent());
						if (channelNumber != 0 && recStatus != 3 && recStatus != 4 && recStatus != 5) {
							err.println("Ignore '" + title + "', may be planned for future or deleted");
							continue;
						}

                        boolean isViewed = item.getElementsByTagName("vx:X_isViewed").item(0).getTextContent().equals("1");
                        if (isViewed) {
                            int viewedMilliseconds = Integer.parseInt(item.getElementsByTagName("vx:X_lastPlaybackPosition").item(0).getTextContent());
                            err.println("'" + title + "' has been watched for " + viewedMilliseconds + " milliseconds");
                            float viewedMinutes = ((float) viewedMilliseconds / 1000 / 60);
                            if (viewedMinutes > 10) {
                                err.println("Ignore '" + title + "', it has been watched");
                                continue;
                            }
                        }

						Node dateNode = item.getElementsByTagName("upnp:recordedStartDateTime").item(0);
						if (dateNode == null)
							dateNode = item.getElementsByTagName("upnp:scheduledStartTime").item(0);
						String dateString = dateNode.getTextContent();
						dateString = dateString.substring(0, 16).replace('T', ' ');
						DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH);
						Date date = format.parse(dateString);

						Recording recording = recordings.get(title);
						if (recording == null)
							recording = new Recording();
						else if (recording.broadcastDate.before(date))
							continue;

						recording.title = title;
						recording.id = item.getElementsByTagName("res").item(0).getTextContent();
						recording.broadcastDate = date;
						recordings.put(title, recording);

						err.println("Examine Title: " + title + ", Date: " + date);
					}
				}
				else if (elem.getTagName().equals("NumberReturned")) {
					returnCount = Integer.parseInt(elem.getTextContent());
					out.println("NumberReturned: " + returnCount);
				}
				else  if (elem.getTagName().equals("TotalMatches")) {
					totalCount = Integer.parseInt(elem.getTextContent());
					out.println("TotalMatches: " + totalCount);
				}
			}
		}

		return (startingIndex + returnCount) < totalCount;
	}

	private void playRecording(Recording recording)
			throws Exception {
		out.println("Play recording");
		String body = String.format(PLAY_BODY, recording.id);
		sendToSky(body, "SetAVTransportURI", true);

        new Thread(new Runnable() {
            @Override
            public void run() {
                // always send the PIN, will fail if not required so ignore error
                try {
                    out.println("Set PIN");
                    Thread.sleep(3000);
                    sendToSky(PIN_BODY, "X_NDS_SetUserPIN", true);
                }
                catch (Exception ignored) { }

                try {
                    // fast forward for a few seconds to try and get almost 2 mins in
                    String body = String.format(SPEED_BODY, "12");
                    sendToSky(body, "Play", true);

                    Thread.sleep(8000);

                    // set back to normal speed
                    body = String.format(SPEED_BODY, "1");
                    sendToSky(body, "Play", true);
                }
                catch (Exception ignored) { }
            }
        }).start();
	}

	private void changeRecordingSpeed(String speed)
			throws Exception {
		String body = String.format(SPEED_BODY, speed);
		sendToSky(body, "Play", true);
	}

	private void pauseRecording()
			throws Exception {
		sendToSky(PAUSE_BODY, "Pause", true);
	}

	private void stopRecording()
			throws Exception {
		sendToSky(STOP_BODY, "Stop", true);
	}

	private String sendToSky(String body, String action, boolean play)
			throws Exception {
		String urlString = play ? SKY_PLAY_URL : SKY_BROWSE_URL;
		URL url = new URL(urlString);

		String soapAction = String.format("\"urn:schemas-nds-com:service:%s:2#%s\"", (play ? "SkyPlay" : "SkyBrowse"), action);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("SOAPACTION", soapAction);
		connection.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
		connection.setRequestProperty("Content-Length", "" + Integer.toString(body.getBytes().length));
		connection.setUseCaches(false);
		connection.setDoInput(true);
		connection.setDoOutput(true); 

		// Send request
		DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
		wr.writeBytes(body);
		wr.flush();
		wr.close();

		out.println("Sending 'POST' request to URL : " + url);
		err.println("with Body " + body);
		int responseCode = connection.getResponseCode();
		out.println("Response Code : " + responseCode);
		if (connection.getErrorStream() != null) {
			InputStreamReader isr = new InputStreamReader(connection.getErrorStream());
			BufferedReader br = new BufferedReader(isr);
			StringBuilder buf = new StringBuilder();
			String line;
			while((line = br.readLine()) != null) {
				buf.append(line);
			}
			isr.close();
			br.close();
			out.println("Error : " + buf.toString());
		}

		BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		String inputLine;
		StringBuilder response = new StringBuilder();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		out.println("Response : " + response.toString());
		return response.toString();
	}

	/**
	 * Calculates the similarity (a number within 0 and 1) between two strings.
	 */
	private static double similarity(String s1, String s2) {
		String longer = s1, shorter = s2;
		if (s1.length() < s2.length()) { // longer should always have greater length
			longer = s2; shorter = s1;
		}
		int longerLength = longer.length();
		if (longerLength == 0) { return 1.0; /* both strings are zero length */ }
    	/* // If you have StringUtils, you can use it to calculate the edit distance:
    		return (longerLength - StringUtils.getLevenshteinDistance(longer, shorter)) /
                           (double) longerLength; */
		return (longerLength - editDistance(longer, shorter)) / (double) longerLength;
	}

	// Example implementation of the Levenshtein Edit Distance
	// See http://rosettacode.org/wiki/Levenshtein_distance#Java
	private static int editDistance(String s1, String s2) {
		s1 = s1.toLowerCase();
		s2 = s2.toLowerCase();

		int[] costs = new int[s2.length() + 1];
		for (int i = 0; i <= s1.length(); i++) {
			int lastValue = i;
			for (int j = 0; j <= s2.length(); j++) {
				if (i == 0)
					costs[j] = j;
				else {
					if (j > 0) {
						int newValue = costs[j - 1];
						if (s1.charAt(i - 1) != s2.charAt(j - 1))
							newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
						costs[j - 1] = lastValue;
						lastValue = newValue;
					}
				}
			}
			if (i > 0)
				costs[s2.length()] = lastValue;
		}
		return costs[s2.length()];
	}

	private static final String BROWSE_BODY = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
			+ "<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\">"
			+ "<s:Body>"
			+ "<u:Browse xmlns:u=\"urn:schemas-nds-com:service:SkyBrowse:2\">"
			+ "<ObjectID>3</ObjectID>"
			+ "<BrowseFlag>BrowseDirectChildren</BrowseFlag>"
			+ "<Filter>res,upnp:channelNr,upnp:recordedStartDateTime,upnp:scheduledStartTime,vx:X_recStatus,vx:X_lastPlaybackPosition,vx:X_isViewed</Filter>"
			+ "<StartingIndex>%s</StartingIndex>"
			+ "<RequestedCount>0</RequestedCount>"
			+ "<SortCriteria></SortCriteria>"
			+ "</u:Browse>"
			+ "</s:Body>"
			+ "</s:Envelope>";

	private static final String PLAY_BODY = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
			+ "<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\">"
			+ "<s:Body>"
			+ "<u:SetAVTransportURI xmlns:u=\"urn:schemas-nds-com:service:SkyPlay:2\">"
			+ "<InstanceID>0</InstanceID>"
			+ "<CurrentURI>%s?position=120&amp;speed=1</CurrentURI>"
			+ "<CurrentURIMetaData>NOT_IMPLEMENTED</CurrentURIMetaData>"
			+ "</u:SetAVTransportURI>"
			+ "</s:Body>"
			+ "</s:Envelope>";

	private static final String PIN_BODY = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
			+ "<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\">"
			+ "<s:Body>"
			+ "<u:X_NDS_SetUserPIN xmlns:u=\"urn:schemas-nds-com:service:SkyPlay:2\">"
			+ "<InstanceID>0</InstanceID>"
			+ "<UserPIN>"
			+ Config.PIN_CODE
			+ "</UserPIN>"
			+ "</u:X_NDS_SetUserPIN>"
			+ "</s:Body>"
			+ "</s:Envelope>";

	private static final String SPEED_BODY = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
			+ "<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\">"
			+ "<s:Body>"
			+ "<u:Play xmlns:u=\"urn:schemas-nds-com:service:SkyPlay:2\">"
			+ "<InstanceID>0</InstanceID>"
			+ "<Speed>%s</Speed>"
			+ "</u:Play>"
			+ "</s:Body>"
			+ "</s:Envelope>";

	private static final String PAUSE_BODY = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
			+ "<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\">"
			+ "<s:Body>"
			+ "<u:Pause xmlns:u=\"urn:schemas-nds-com:service:SkyPlay:2\">"
			+ "<InstanceID>0</InstanceID>"
			+ "</u:Pause>"
			+ "</s:Body>"
			+ "</s:Envelope>";

	private static final String STOP_BODY = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
			+ "<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\">"
			+ "<s:Body>"
			+ "<u:Stop xmlns:u=\"urn:schemas-nds-com:service:SkyPlay:2\">"
			+ "<InstanceID>0</InstanceID>"
			+ "</u:Stop>"
			+ "</s:Body>"
			+ "</s:Envelope>";

	/**
	 * Creates a {@code SpeechletResponse} for the help intent.
	 *
	 * @return SpeechletResponse spoken and visual response for the given intent
	 */
	private SpeechletResponse getHelpResponse() {
		String speechText = "I am not a very helpful skill I'm afraid!";

		// Create the Simple card content.
		SimpleCard card = new SimpleCard();
		card.setTitle("Sky");
		card.setContent(speechText);

		// Create the plain text output.
		PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
		speech.setText(speechText);

		// Create reprompt
		Reprompt reprompt = new Reprompt();
		reprompt.setOutputSpeech(speech);

		return SpeechletResponse.newAskResponse(speech, reprompt, card);
	}
}