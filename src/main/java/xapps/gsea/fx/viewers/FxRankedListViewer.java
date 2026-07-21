/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers;

import org.gsea_msigdb.gsea.ui.api.ViewPage;

import edu.mit.broad.genome.objects.RankedList;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Ranked gene list: name and score.
 */
public class FxRankedListViewer implements ViewPage {

    private final RankedList rankedList;
    private final BorderPane root = new BorderPane();

    public FxRankedListViewer(RankedList rankedList) {
        this.rankedList = rankedList;

        Label title = new Label(rankedList.getName() + "  (" + rankedList.getSize() + " genes)");
        title.setPadding(new Insets(8, 12, 4, 12));
        title.getStyleClass().add("gsea-section-header");

        TableView<Row> table = new TableView<>();
        TableColumn<Row, String> nameCol = new TableColumn<>("Feature Name");
        nameCol.setCellValueFactory(c -> c.getValue().name);
        nameCol.setPrefWidth(220);
        TableColumn<Row, String> rankCol = new TableColumn<>("Rank");
        rankCol.setCellValueFactory(c -> c.getValue().rank);
        rankCol.setPrefWidth(80);
        TableColumn<Row, String> scoreCol = new TableColumn<>("Score");
        scoreCol.setCellValueFactory(c -> c.getValue().score);
        scoreCol.setPrefWidth(120);
        table.getColumns().add(nameCol);
        table.getColumns().add(rankCol);
        table.getColumns().add(scoreCol);

        ObservableList<Row> rows = FXCollections.observableArrayList();
        for (int i = 0; i < rankedList.getSize(); i++) {
            rows.add(new Row(rankedList.getRankName(i), Integer.toString(i + 1), Float.toString(rankedList.getScore(i))));
        }
        table.setItems(rows);

        VBox box = new VBox(4, title, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        root.setCenter(box);
    }

    private static final class Row {
        final SimpleStringProperty name;
        final SimpleStringProperty rank;
        final SimpleStringProperty score;

        Row(String name, String rank, String score) {
            this.name = new SimpleStringProperty(name);
            this.rank = new SimpleStringProperty(rank);
            this.score = new SimpleStringProperty(score);
        }
    }

    @Override
    public String getTitle() {
        return rankedList.getName();
    }

    @Override
    public String getIconResourceId() {
        return "Rnk.png";
    }

    @Override
    public Object getContent() {
        return root;
    }
}
