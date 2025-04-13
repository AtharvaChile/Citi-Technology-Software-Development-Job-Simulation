//package org.example;
//
//import java.io.BufferedReader;
//import java.io.InputStreamReader;
//import java.net.HttpURLConnection;
//import java.net.URL;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.LinkedList;
//import java.util.Queue;
//import org.json.JSONObject;
//
//public class App {
//    // The base URL for the Yahoo Finance API
//    private static final String BASE_URL = "https://yfapi.net/v6/finance/quote/marketSummary?lang=en&region=US";
//    private static final String SYMBOL = "DJI"; // Dow Jones Industrial Average symbol
//    private static final String API_KEY = "BBY6PaIxJk3oiTXIi2J404IOPtLKh8pZ8ciQgY9e"; // Replace with your actual API key
//    private static final int WAIT_TIME_MS = 5000;
//
//    public static void main(String[] args) {
//        // Queue for storing stock data with timestamps
//        Queue<ArrayList<Object>> stockDataQueue = new LinkedList<>();
//
//        while (true) {
//            try {
//                // Fetch stock data from the API
//                String jsonResponse = fetchStockData(SYMBOL);
//                Date timestamp = new Date();
//
//                // Parse the JSON response to extract the relevant data (stock price)
//                JSONObject responseJson = new JSONObject(jsonResponse);
//                double price = responseJson.getJSONObject("marketSummaryResponse")
//                                           .getJSONArray("result")
//                                           .getJSONObject(1) // DJI data is the second element in the array
//                                           .getJSONObject("regularMarketPrice")
//                                           .getDouble("raw");
//
//                // Store the stock price and timestamp in the queue
//                ArrayList<Object> stockData = new ArrayList<>();
//                stockData.add(timestamp);
//                stockData.add(price); // Store the stock price
//                stockDataQueue.add(stockData);
//
//                // Print only the relevant information: timestamp and stock price
//                System.out.println("Timestamp: " + timestamp + ", Dow Jones Price: " + price);
//            } catch (Exception e) {
//                System.err.println("Error fetching stock data: " + e.getMessage());
//            }
//
//            // Wait for a specified time before making the next request
//            try {
//                Thread.sleep(WAIT_TIME_MS);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    private static String fetchStockData(String symbol) throws Exception {
//        // Construct the URL with the correct API endpoint for DJI
//        String urlString = BASE_URL + symbol;
//        URL url = new URL(urlString);
//
//        // Set up the HTTP connection
//        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//        connection.setRequestMethod("GET");
//        connection.setRequestProperty("Accept", "application/json");
//        connection.setRequestProperty("X-API-KEY", API_KEY);
//
//        // Get the response code and handle the response
//        int responseCode = connection.getResponseCode();
//        if (responseCode == HttpURLConnection.HTTP_OK) {
//            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//            StringBuilder response = new StringBuilder();
//            String line;
//            while ((line = in.readLine()) != null) {
//                response.append(line);
//            }
//            in.close();
//            return response.toString();
//        } else {
//            throw new Exception("HTTP Error: " + responseCode);
//        }
//    }
//}
//

package org.example;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

public class App extends Application {
    private static final String BASE_URL = "https://yfapi.net/v6/finance/quote/marketSummary?lang=en&region=US";
    private static final String SYMBOL = "DJI"; // Dow Jones Industrial Average symbol
    private static final String API_KEY = "BBY6PaIxJk3oiTXIi2J404IOPtLKh8pZ8ciQgY9e"; // Replace with your actual API key
    private static final int WAIT_TIME_MS = 5000;

    private static final Queue<XYChart.Data<Number, Number>> stockDataQueue = new LinkedList<>();
    private static XYChart.Series<Number, Number> series;

    @Override
    public void start(Stage primaryStage) {
        // Set up the x and y axes
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Time (seconds)");
        
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Stock Price");

        // Create the line chart
        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Stock Price Over Time");

        // Initialize the series for the chart
        series = new XYChart.Series<>();
        series.setName("Dow Jones Price");
        lineChart.getData().add(series);

        // Set up the scene and stage
        Scene scene = new Scene(lineChart, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Stock Price Tracker");
        primaryStage.show();

        // Start the data-fetching thread
        new Thread(this::fetchStockDataContinuously).start();
    }

    private void fetchStockDataContinuously() {
        long startTime = System.currentTimeMillis();

        while (true) {
            try {
                // Fetch stock data from the API
                String jsonResponse = fetchStockData(SYMBOL);
                double price = parseStockPrice(jsonResponse);

                // Calculate elapsed time in seconds
                long currentTime = System.currentTimeMillis();
                long elapsedTime = (currentTime - startTime) / 1000;

                // Add the data to the series on the JavaFX Application Thread
                Platform.runLater(() -> {
                    series.getData().add(new XYChart.Data<>(elapsedTime, price));

                    // Optionally, limit the number of displayed points
                    if (series.getData().size() > 50) {
                        series.getData().remove(0);
                    }
                });

                System.out.println("Time: " + elapsedTime + "s, Price: " + price);

                // Wait before fetching the next data point
                Thread.sleep(WAIT_TIME_MS);
            } catch (Exception e) {
                System.err.println("Error fetching stock data: " + e.getMessage());
            }
        }
    }

    private String fetchStockData(String symbol) throws Exception {
        String urlString = BASE_URL;
        URL url = new URL(urlString);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("X-API-KEY", API_KEY);

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();
            return response.toString();
        } else {
            throw new Exception("HTTP Error: " + responseCode);
        }
    }

    private double parseStockPrice(String jsonResponse) {
        JSONObject responseJson = new JSONObject(jsonResponse);
        return responseJson.getJSONObject("marketSummaryResponse")
                .getJSONArray("result")
                .getJSONObject(1) // DJI data is the second element in the array
                .getJSONObject("regularMarketPrice")
                .getDouble("raw");
    }

    public static void main(String[] args) {
        launch(args);
    }
}

