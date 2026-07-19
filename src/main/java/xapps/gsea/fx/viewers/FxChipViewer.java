/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers;

import org.gsea_msigdb.gsea.ui.api.ViewPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.vdb.chip.Chip;
import edu.mit.broad.vdb.chip.NullSymbolModes;
import edu.mit.broad.vdb.chip.Probe;
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
 * Chip annotation: probe → symbol (and title).
 */
public class FxChipViewer implements ViewPage {

    private static final Logger klog = LoggerFactory.getLogger(FxChipViewer.class);

    private final Chip chip;
    private final BorderPane root = new BorderPane();

    public FxChipViewer(Chip chip) {
        this.chip = chip;

        int total = 0;
        ObservableList<Row> rows = FXCollections.observableArrayList();
        try {
            total = chip.getNumProbes();
            for (int i = 0; i < total; i++) {
                Probe p = chip.getProbe(i);
                String probeName = p.getName();
                rows.add(new Row(i + 1, probeName,
                        chip.getSymbol(probeName, NullSymbolModes.OmitNulls),
                        chip.getTitle(probeName, NullSymbolModes.OmitNulls)));
            }
        } catch (Exception e) {
            klog.error("Failed to load chip probes for {}", chip.getName(), e);
        }

        Label title = new Label(chip.getName() + "  (" + total + " probes)");
        title.setPadding(new Insets(8, 12, 4, 12));
        title.getStyleClass().add("gsea-section-header");

        TableView<Row> table = new TableView<>(rows);
        TableColumn<Row, String> numCol = new TableColumn<>(" ");
        numCol.setCellValueFactory(c -> c.getValue().index);
        numCol.setPrefWidth(50);
        TableColumn<Row, String> probeCol = new TableColumn<>("FEATURE");
        probeCol.setCellValueFactory(c -> c.getValue().probe);
        probeCol.setPrefWidth(160);
        TableColumn<Row, String> symbolCol = new TableColumn<>("SYMBOL");
        symbolCol.setCellValueFactory(c -> c.getValue().symbol);
        symbolCol.setPrefWidth(120);
        TableColumn<Row, String> titleCol = new TableColumn<>("TITLE");
        titleCol.setCellValueFactory(c -> c.getValue().title);
        titleCol.setPrefWidth(360);
        table.getColumns().add(numCol);
        table.getColumns().add(probeCol);
        table.getColumns().add(symbolCol);
        table.getColumns().add(titleCol);

        VBox box = new VBox(4, title, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        root.setCenter(box);
    }

    private static final class Row {
        final SimpleStringProperty index;
        final SimpleStringProperty probe;
        final SimpleStringProperty symbol;
        final SimpleStringProperty title;

        Row(int index, String probe, String symbol, String title) {
            this.index = new SimpleStringProperty(Integer.toString(index));
            this.probe = new SimpleStringProperty(probe);
            this.symbol = new SimpleStringProperty(symbol);
            this.title = new SimpleStringProperty(title);
        }
    }

    @Override
    public String getTitle() {
        return chip.getName();
    }

    @Override
    public String getIconResourceId() {
        return "Chip16.png";
    }

    @Override
    public Object getContent() {
        return root;
    }
}
