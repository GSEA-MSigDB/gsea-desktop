/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.report;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/**
 * Read-only parameter name/value table shared by the report viewer and Analysis History.
 */
public final class ReportParamsTable {

    private ReportParamsTable() {
    }

    public static TableView<Row> create(Properties params) {
        TableView<Row> table = new TableView<>();
        TableColumn<Row, String> nameCol = new TableColumn<>("Parameter name");
        nameCol.setCellValueFactory(c -> c.getValue().nameProperty());
        nameCol.setPrefWidth(220);
        TableColumn<Row, String> valueCol = new TableColumn<>("Parameter value");
        valueCol.setCellValueFactory(c -> c.getValue().valueProperty());
        valueCol.setPrefWidth(420);
        table.getColumns().addAll(nameCol, valueCol);
        table.setEditable(false);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label(""));

        List<Row> rows = new ArrayList<>();
        if (params != null) {
            for (String key : params.stringPropertyNames()) {
                rows.add(new Row(key, params.getProperty(key)));
            }
        }
        table.setItems(FXCollections.observableArrayList(rows));
        return table;
    }

    public static final class Row {
        private final SimpleStringProperty name = new SimpleStringProperty();
        private final SimpleStringProperty value = new SimpleStringProperty();

        Row(String name, String value) {
            this.name.set(name);
            this.value.set(value != null ? value : "");
        }

        public SimpleStringProperty nameProperty() {
            return name;
        }

        public SimpleStringProperty valueProperty() {
            return value;
        }
    }
}
