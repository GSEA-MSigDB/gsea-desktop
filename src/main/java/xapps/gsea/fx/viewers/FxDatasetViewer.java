/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers;

import java.util.stream.IntStream;

import org.gsea_msigdb.gsea.ui.api.ViewPage;

import edu.mit.broad.genome.objects.Dataset;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Dataset expression table (all sample columns) plus an Info tab.
 * Wide matrices (e.g. Leading Edge gene-set × gene membership) are shown
 * transposed so TableView stays on a manageable column count.
 */
public class FxDatasetViewer implements ViewPage {

    /** JavaFX TableView freezes when forced to create thousands of columns. */
    private static final int MAX_EAGER_COLUMNS = 64;
    /** Cap Info-tab name dumps for wide matrices. */
    private static final int MAX_INFO_NAMES = 200;

    private final Dataset dataset;
    private final boolean transposed;
    private final BorderPane root = new BorderPane();

    public FxDatasetViewer(Dataset dataset) {
        this.dataset = dataset;
        int numRows = dataset.getNumRow();
        int numCols = dataset.getNumCol();
        // Prefer many virtualized rows over many TableColumns (LE membership GCTs).
        this.transposed = shouldTranspose(numRows, numCols);

        TableView<Integer> table = transposed ? buildTransposedTable() : buildNaturalTable();

        TextArea infoArea = new TextArea(buildInfo(dataset, transposed));
        infoArea.setEditable(false);
        infoArea.setWrapText(true);

        TabPane tabs = new TabPane();
        tabs.setSide(javafx.geometry.Side.BOTTOM);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        Tab dataTab = new Tab("Data", table);
        dataTab.setClosable(false);
        Tab infoTab = new Tab("Info", infoArea);
        infoTab.setClosable(false);
        tabs.getTabs().addAll(dataTab, infoTab);

        VBox box = new VBox(4, tabs);
        VBox.setVgrow(tabs, Priority.ALWAYS);
        root.setCenter(box);
    }

    private static boolean shouldTranspose(int numRows, int numCols) {
        if (numCols <= MAX_EAGER_COLUMNS) {
            return false;
        }
        // Choose the orientation with fewer TableColumns.
        return numCols > numRows;
    }

    private TableView<Integer> buildNaturalTable() {
        int numRows = dataset.getNumRow();
        int numCols = dataset.getNumCol();

        TableView<Integer> table = newTable();
        addIndexColumn(table);
        addNameColumn(table, "Feature", r -> dataset.getRowName(r));

        int colLimit = Math.min(numCols, MAX_EAGER_COLUMNS);
        for (int c = 0; c < colLimit; c++) {
            final int col = c;
            addValueColumn(table, dataset.getColumnName(c), r -> dataset.getElement(r, col));
        }
        maybeAppendTruncationColumn(table, numCols - colLimit);

        table.setItems(FXCollections.observableArrayList(
                IntStream.range(0, numRows).boxed().toList()));
        return table;
    }

    /**
     * Display original columns as rows and original rows as columns.
     * Used when the natural orientation would create too many TableColumns.
     */
    private TableView<Integer> buildTransposedTable() {
        int numOrigRows = dataset.getNumRow();
        int numOrigCols = dataset.getNumCol();

        TableView<Integer> table = newTable();
        addIndexColumn(table);
        addNameColumn(table, "Feature", origCol -> dataset.getColumnName(origCol));

        int colLimit = Math.min(numOrigRows, MAX_EAGER_COLUMNS);
        for (int r = 0; r < colLimit; r++) {
            final int origRow = r;
            addValueColumn(table, dataset.getRowName(r),
                    origCol -> dataset.getElement(origRow, origCol));
        }
        maybeAppendTruncationColumn(table, numOrigRows - colLimit);

        table.setItems(FXCollections.observableArrayList(
                IntStream.range(0, numOrigCols).boxed().toList()));
        return table;
    }

    private static TableView<Integer> newTable() {
        TableView<Integer> table = new TableView<>();
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label(""));
        return table;
    }

    private static void addIndexColumn(TableView<Integer> table) {
        TableColumn<Integer, String> numCol = new TableColumn<>(" ");
        numCol.setCellValueFactory(cdf -> new SimpleStringProperty(Integer.toString(cdf.getValue() + 1)));
        numCol.setPrefWidth(50);
        table.getColumns().add(numCol);
    }

    private static void addNameColumn(TableView<Integer> table, String title,
            java.util.function.IntFunction<String> nameAt) {
        TableColumn<Integer, String> col = new TableColumn<>(title);
        col.setCellValueFactory(cdf -> new SimpleStringProperty(nameAt.apply(cdf.getValue())));
        col.setPrefWidth(140);
        table.getColumns().add(col);
    }

    private static void addValueColumn(TableView<Integer> table, String title,
            java.util.function.IntFunction<Float> valueAt) {
        TableColumn<Integer, String> col = new TableColumn<>(title);
        col.setCellValueFactory(cdf -> new SimpleStringProperty(
                Float.toString(valueAt.apply(cdf.getValue()))));
        col.setPrefWidth(80);
        table.getColumns().add(col);
    }

    private static void maybeAppendTruncationColumn(TableView<Integer> table, int omitted) {
        if (omitted <= 0) {
            return;
        }
        TableColumn<Integer, String> more = new TableColumn<>("… +" + omitted + " more");
        more.setCellValueFactory(cdf -> new SimpleStringProperty(""));
        more.setPrefWidth(100);
        table.getColumns().add(more);
    }

    private static String buildInfo(Dataset ds, boolean transposed) {
        StringBuilder buf = new StringBuilder();
        buf.append("Name: ").append(ds.getName()).append('\n');
        buf.append("Number of rows(features): ").append(ds.getNumRow()).append('\n');
        buf.append("Num of columns(samples): ").append(ds.getNumCol()).append('\n');
        if (transposed) {
            buf.append("\nDisplay: transposed (original columns shown as rows) so the table stays responsive.\n");
        }

        String comm = ds.getComment();
        if (comm != null && !comm.isEmpty()) {
            buf.append("\nComments\n");
            buf.append(comm);
        }

        buf.append("\nColumn Names\n");
        int n = ds.getNumCol();
        int limit = Math.min(n, MAX_INFO_NAMES);
        for (int c = 0; c < limit; c++) {
            buf.append(ds.getColumnName(c)).append('\n');
        }
        if (n > limit) {
            buf.append("… and ").append(n - limit).append(" more\n");
        }
        return buf.toString();
    }

    @Override
    public String getTitle() {
        return dataset.getName();
    }

    @Override
    public String getIconResourceId() {
        return "Dataset16.gif";
    }

    @Override
    public javafx.scene.Node getContent() {
        return root;
    }
}
