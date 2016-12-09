/**
    Copyright 2014-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.

    Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with the License. A copy of the License is located at

        http://aws.amazon.com/apache2.0/

    or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

import config.Config;
import sky.SkySpeechlet;
import com.amazon.speech.Sdk;
import com.amazon.speech.speechlet.Speechlet;
import com.amazon.speech.speechlet.servlet.SpeechletServlet;
import org.apache.log4j.BasicConfigurator;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.io.*;
import java.util.Properties;

import static java.lang.System.out;

/**
 * Shared launcher for executing all sample skills within a single servlet container.
 */
public final class Launcher {
    private static final Logger log = LoggerFactory.getLogger(Launcher.class);

    private static Thread discovery;
    /**
     * port number for the jetty server.
     */
    private static final int PORT = 8889;

    /**
     * Security scheme to use.
     */
    private static final String HTTPS_SCHEME = "https";

    /**
     * default constructor.
     */
    private Launcher() {
    }

    /**
     * Main entry point. Starts a Jetty server.
     *
     * @param args
     *            ignored.
     * @throws Exception
     *             if anything goes wrong.
     */
    public static void main(final String[] args) throws Exception {
        // Configure logging to output to the console with default level of INFO
        BasicConfigurator.configure();

        Config.loadProperties();

        // Configure server and its associated servlets
        Server server = new Server();
        SslConnectionFactory sslConnectionFactory = new SslConnectionFactory();
        SslContextFactory sslContextFactory = sslConnectionFactory.getSslContextFactory();
        sslContextFactory.setKeyStorePath(System.getProperty("javax.net.ssl.keyStore"));
        sslContextFactory.setKeyStorePassword(System.getProperty("javax.net.ssl.keyStorePassword"));
        sslContextFactory.setIncludeCipherSuites(Sdk.SUPPORTED_CIPHER_SUITES);

        HttpConfiguration httpConf = new HttpConfiguration();
        httpConf.setSecurePort(PORT);
        httpConf.setSecureScheme(HTTPS_SCHEME);
        httpConf.addCustomizer(new SecureRequestCustomizer());
        HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory(httpConf);

        ServerConnector serverConnector =
                new ServerConnector(server, sslConnectionFactory, httpConnectionFactory);
        serverConnector.setPort(PORT);

        Connector[] connectors = new Connector[1];
        connectors[0] = serverConnector;
        server.setConnectors(connectors);

        SkySpeechlet sky = new SkySpeechlet();
        discoverSkyBox(sky);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
        context.addServlet(new ServletHolder(createServlet(sky)), "/sky");
        server.start();
        server.join();
    }

    private static void discoverSkyBox(final SkySpeechlet sky) {
        discovery = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] sendData = new byte[1024];
                byte[] receiveData = new byte[1024];

                String MSEARCH = "M-SEARCH * HTTP/1.1\nHost: 239.255.255.250:1900\nMan: \"ssdp:discover\"\nST: ssdp:all\nMX: 5\n\n";
                sendData = MSEARCH.getBytes();

                try {
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("239.255.255.250"), 1900);

                    // send packet to the socket we're creating
                    DatagramSocket clientSocket = new DatagramSocket();
                    clientSocket.send(sendPacket);

                    // receive response and store in our receivePacket
                    DatagramPacket receivePacket;
                    boolean playReceived = false, browseReceived = false;
                    while (true) {
                        try {
                            receivePacket = new DatagramPacket(receiveData, receiveData.length);
                            clientSocket.receive(receivePacket);

                            // get the response as a string
                            String response = new String(receivePacket.getData());
                            if ((response.contains("SkyPlay") || response.contains("SkyBrowse"))) {
                                log.info("SSDP Response");

                                String ip = response.substring(response.indexOf("http://") + 7, response.indexOf(":", response.indexOf("http://") + 7));
                                String uuid = response.substring(response.indexOf("uuid:") + 5, response.indexOf(":", response.indexOf("uuid:") + 5));
                                if (response.contains("SkyPlay")) {
                                    playReceived = true;
                                    sky.setSkyPlayUrl(ip, uuid);
                                }
                                else {
                                    browseReceived = true;
                                    sky.setSkyBrowseUrl(ip, uuid);
                                }
                            }

                            if (playReceived && browseReceived) {
                                log.info("All Sky Urls discovered");
                                break;
                            }
                        } catch (Exception e) {
                            log.info(e.toString());
                            break;
                        }
                    }

                    // close the socket
                    clientSocket.close();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        discovery.start();
    }

    private static SpeechletServlet createServlet(final Speechlet speechlet) {
        SpeechletServlet servlet = new SpeechletServlet();
        servlet.setSpeechlet(speechlet);
        return servlet;
    }
}
