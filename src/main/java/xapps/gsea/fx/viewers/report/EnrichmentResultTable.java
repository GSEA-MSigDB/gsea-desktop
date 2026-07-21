/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.report;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentResult;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

/**
 * Shared enrichment-result table wiring (columns, sort, optional name filter).
 */
public final class EnrichmentResultTable {

    private final ObservableList<EnrichmentResultRow> allRows = FXCollections.observableArrayList();
    private final FilteredList<EnrichmentResultRow> filteredRows = new FilteredList<>(allRows, r -> true);
    private final SortedList<EnrichmentResultRow> sortedRows = new SortedList<>(filteredRows);
    private final TableView<EnrichmentResultRow> table = new TableView<>(sortedRows);

    public EnrichmentResultTable() {
        this(SelectionMode.MULTIPLE);
    }

    public EnrichmentResultTable(SelectionMode selectionMode) {
        buildColumns();
        sortedRows.comparatorProperty().bind(table.comparatorProperty());
        table.getSelectionModel().setSelectionMode(
                selectionMode != null ? selectionMode : SelectionMode.MULTIPLE);
        table.setPlaceholder(new Label("No gene sets"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getStyleClass().add("gsea-enrichment-table");
    }

    public TableView<EnrichmentResultRow> getTable() {
        return table;
    }

    public void setResults(EnrichmentResult[] results) {
        allRows.setAll(toRows(results));
    }

    public void setNameFilter(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        Predicate<EnrichmentResultRow> pred = r -> {
            if (q.isEmpty()) {
                return true;
            }
            String name = r.nameProperty().get();
            return name != null && name.toLowerCase(Locale.ROOT).contains(q);
        };
        filteredRows.setPredicate(pred);
    }

    /** Bind this table's filter to an existing field (shared across phenotype panels). */
    public void bindFilterField(TextField filter) {
        if (filter == null) {
            return;
        }
        filter.textProperty().addListener((obs, o, n) -> setNameFilter(n));
        setNameFilter(filter.getText());
    }

    public void clearSelection() {
        table.getSelectionModel().clearSelection();
    }

    private void buildColumns() {
        TableColumn<EnrichmentResultRow, String> nameCol = new TableColumn<>("Gene Set");
        nameCol.setCellValueFactory(c -> c.getValue().nameProperty());
        nameCol.setPrefWidth(220);

        TableColumn<EnrichmentResultRow, Number> sizeCol = new TableColumn<>("Size");
        sizeCol.setCellValueFactory(c -> c.getValue().sizeProperty());
        sizeCol.setPrefWidth(55);

        TableColumn<EnrichmentResultRow, Number> esCol = new TableColumn<>("ES");
        esCol.setCellValueFactory(c -> c.getValue().esProperty());
        esCol.setPrefWidth(70);

        TableColumn<EnrichmentResultRow, Number> nesCol = new TableColumn<>("NES");
        nesCol.setCellValueFactory(c -> c.getValue().nesProperty());
        nesCol.setPrefWidth(70);

        TableColumn<EnrichmentResultRow, Number> nomCol = new TableColumn<>("NOM p-Val");
        nomCol.setCellValueFactory(c -> c.getValue().nomPProperty());
        nomCol.setPrefWidth(75);

        TableColumn<EnrichmentResultRow, Number> fdrCol = new TableColumn<>("FDR");
        fdrCol.setCellValueFactory(c -> c.getValue().fdrProperty());
        fdrCol.setPrefWidth(75);

        TableColumn<EnrichmentResultRow, Number> fwerCol = new TableColumn<>("FWER");
        fwerCol.setCellValueFactory(c -> c.getValue().fwerProperty());
        fwerCol.setPrefWidth(75);

        TableColumn<EnrichmentResultRow, Number> rankCol = new TableColumn<>("Rank at Max");
        rankCol.setCellValueFactory(c -> c.getValue().rankAtMaxProperty());
        rankCol.setPrefWidth(90);

        TableColumn<EnrichmentResultRow, String> leCol = new TableColumn<>("Leading Edge");
        leCol.setCellValueFactory(c -> c.getValue().leadingEdgeProperty());
        leCol.setPrefWidth(180);

        table.getColumns().addAll(nameCol, sizeCol, esCol, nesCol, nomCol, fdrCol, fwerCol, rankCol, leCol);
    }

    private static List<EnrichmentResultRow> toRows(EnrichmentResult[] results) {
        List<EnrichmentResultRow> rows = new ArrayList<>();
        if (results != null) {
            for (EnrichmentResult r : results) {
                rows.add(new EnrichmentResultRow(r));
            }
        }
        return rows;
    }
}
