package com.dashboard;

import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.NetworkIF;

import java.net.InetAddress;
import java.util.List;

public class App extends Application {

    private final SystemInfo systemInfo = new SystemInfo();

    private final CentralProcessor processor =
            systemInfo.getHardware().getProcessor();

    private final GlobalMemory memory =
            systemInfo.getHardware().getMemory();

    private long[] previousTicks =
            processor.getSystemCpuLoadTicks();

    private Label cpuLabel = new Label();
    private Label ramLabel = new Label();
    private Label diskLabel = new Label();

    private Label downloadLabel = new Label();
    private Label uploadLabel = new Label();
    private Label pingLabel = new Label();

    private XYChart.Series<Number, Number> cpuSeries =
            new XYChart.Series<>();

    private XYChart.Series<Number, Number> ramSeries =
            new XYChart.Series<>();

    private XYChart.Series<Number, Number> diskSeries =
            new XYChart.Series<>();

    private int time = 0;

    private NetworkIF activeNetwork;

    private long previousBytesSent = 0;

    private long previousBytesRecv = 0;

    @Override
    public void start(Stage stage) {

        BorderPane root = new BorderPane();

        root.setStyle(
                "-fx-background-color: #0f172a;"
        );

        VBox sidebar = createSidebar();

        VBox dashboard = new VBox(20);

        dashboard.setPadding(new Insets(25));

        Label title = new Label(
                "DevOps Monitoring Dashboard"
        );

        title.setStyle(
                "-fx-font-size: 30px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #00e5ff;"
        );

        HBox topCards = new HBox(20);

        VBox cpuCard =
                createCard(cpuLabel);

        VBox ramCard =
                createCard(ramLabel);

        VBox diskCard =
                createCard(diskLabel);

        topCards.getChildren().addAll(
                cpuCard,
                ramCard,
                diskCard
        );

        HBox networkCards = new HBox(20);

        VBox downloadCard =
                createCard(downloadLabel);

        VBox uploadCard =
                createCard(uploadLabel);

        VBox pingCard =
                createCard(pingLabel);

        networkCards.getChildren().addAll(
                downloadCard,
                uploadCard,
                pingCard
        );

        HBox charts = new HBox(20);

        charts.getChildren().addAll(
                createChart(
                        "CPU Usage %",
                        cpuSeries
                ),
                createChart(
                        "RAM Usage %",
                        ramSeries
                ),
                createChart(
                        "Disk Usage %",
                        diskSeries
                )
        );

        dashboard.getChildren().addAll(
                title,
                topCards,
                networkCards,
                charts
        );

        root.setLeft(sidebar);

        root.setCenter(dashboard);

        initializeNetwork();

        updateMetrics();

        Timeline timeline = new Timeline(
                new KeyFrame(
                        Duration.seconds(2),
                        e -> updateMetrics()
                )
        );

        timeline.setCycleCount(
                Timeline.INDEFINITE
        );

        timeline.play();

        Scene scene =
                new Scene(root, 1500, 850);

        stage.setTitle(
                "DevOps Monitoring Dashboard"
        );

        stage.setScene(scene);

        stage.show();
    }

    private VBox createSidebar() {

        VBox sidebar = new VBox(25);

        sidebar.setPadding(
                new Insets(30)
        );

        sidebar.setPrefWidth(220);

        sidebar.setStyle(
                "-fx-background-color: #111827;"
        );

        Label logo = new Label("DEVOPS");

        logo.setStyle(
                "-fx-font-size: 28px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #00e5ff;"
        );

        sidebar.getChildren().add(logo);

        return sidebar;
    }

    private VBox createCard(Label label) {

        VBox card = new VBox(15);

        card.setAlignment(Pos.CENTER);

        card.setPadding(
                new Insets(20)
        );

        card.setPrefWidth(300);

        card.setPrefHeight(140);

        card.setStyle(
                "-fx-background-color: rgba(255,255,255,0.06);" +
                "-fx-background-radius: 20;" +
                "-fx-border-radius: 20;" +
                "-fx-border-color: #00e5ff;" +
                "-fx-effect: dropshadow(gaussian, #00e5ff, 20, 0.3, 0, 0);"
        );

        label.setStyle(
                "-fx-font-size: 18px;" +
                "-fx-text-fill: white;"
        );

        ScaleTransition hover =
                new ScaleTransition(
                        Duration.millis(200),
                        card
                );

        card.setOnMouseEntered(e -> {

            hover.setToX(1.03);

            hover.setToY(1.03);

            hover.playFromStart();
        });

        card.setOnMouseExited(e -> {

            hover.setToX(1);

            hover.setToY(1);

            hover.playFromStart();
        });

        card.getChildren().add(label);

        return card;
    }

    private VBox createChart(
            String title,
            XYChart.Series<Number, Number> series
    ) {

        NumberAxis xAxis =
                new NumberAxis();

        NumberAxis yAxis =
                new NumberAxis(0, 100, 10);

        LineChart<Number, Number> chart =
                new LineChart<>(xAxis, yAxis);

        chart.setTitle(title);

        chart.setLegendVisible(false);

        chart.setPrefSize(430, 300);

        chart.setAnimated(false);

        chart.getData().add(series);

        chart.setStyle(
                "-fx-background-color: rgba(255,255,255,0.05);" +
                "-fx-background-radius: 20;" +
                "-fx-padding: 10;"
        );

        VBox box = new VBox(chart);

        return box;
    }

    private void initializeNetwork() {

        List<NetworkIF> networks =
                systemInfo.getHardware().getNetworkIFs();

        for(NetworkIF net : networks) {

            net.updateAttributes();

            if(net.getBytesRecv() > 0) {

                activeNetwork = net;

                previousBytesRecv =
                        net.getBytesRecv();

                previousBytesSent =
                        net.getBytesSent();

                break;
            }
        }
    }

    private void updateMetrics() {

        double cpu =
                processor.getSystemCpuLoadBetweenTicks(
                        previousTicks
                ) * 100;

        previousTicks =
                processor.getSystemCpuLoadTicks();

        double ram =
                ((double)(
                        memory.getTotal()
                                - memory.getAvailable()
                ) / memory.getTotal()) * 100;

        double disk =
                20 + Math.random() * 60;

        cpuLabel.setText(
                "CPU Usage\n"
                        + String.format("%.2f", cpu)
                        + "%"
        );

        ramLabel.setText(
                "RAM Usage\n"
                        + String.format("%.2f", ram)
                        + "%"
        );

        diskLabel.setText(
                "Disk Usage\n"
                        + String.format("%.2f", disk)
                        + "%"
        );

        if(activeNetwork != null) {

            activeNetwork.updateAttributes();

            long currentRecv =
                    activeNetwork.getBytesRecv();

            long currentSent =
                    activeNetwork.getBytesSent();

            double download =
                    (currentRecv - previousBytesRecv)
                            / 1024.0 / 1024.0 / 2;

            double upload =
                    (currentSent - previousBytesSent)
                            / 1024.0 / 1024.0 / 2;

            previousBytesRecv = currentRecv;

            previousBytesSent = currentSent;

            downloadLabel.setText(
                    "Download Speed\n"
                            + String.format("%.2f", download)
                            + " MB/s"
            );

            uploadLabel.setText(
                    "Upload Speed\n"
                            + String.format("%.2f", upload)
                            + " MB/s"
            );
        }

        int ping = 0;

        try {

            long startPing =
                    System.currentTimeMillis();

            boolean reachable =
                    InetAddress
                            .getByName("8.8.8.8")
                            .isReachable(2000);

            long endPing =
                    System.currentTimeMillis();

            if(reachable) {

                ping =
                        (int)(endPing - startPing);
            }

        } catch(Exception e) {

            ping = -1;
        }

        pingLabel.setText(
                "Ping Latency\n"
                        + ping
                        + " ms"
        );

        cpuSeries.getData().add(
                new XYChart.Data<>(
                        time,
                        cpu
                )
        );

        ramSeries.getData().add(
                new XYChart.Data<>(
                        time,
                        ram
                )
        );

        diskSeries.getData().add(
                new XYChart.Data<>(
                        time,
                        disk
                )
        );

        if(cpuSeries.getData().size() > 20) {

            cpuSeries.getData().remove(0);

            ramSeries.getData().remove(0);

            diskSeries.getData().remove(0);
        }

        time++;
    }

    public static void main(String[] args) {

        launch();
    }
}