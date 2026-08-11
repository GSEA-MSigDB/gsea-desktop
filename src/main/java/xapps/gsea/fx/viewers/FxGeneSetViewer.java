/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers;

import org.gsea_msigdb.gsea.ui.api.ViewPage;

import edu.mit.broad.genome.objects.GeneSet;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Gene set member list.
 */
public class FxGeneSetViewer implements ViewPage {

    private final GeneSet geneSet;
    private final BorderPane root = new BorderPane();

    public FxGeneSetViewer(GeneSet geneSet) {
        this.geneSet = geneSet;

        TableView<Row> table = new TableView<>();
        TableColumn<Row, String> numCol = new TableColumn<>(" ");
        numCol.setCellValueFactory(c -> c.getValue().index);
        numCol.setPrefWidth(50);
        TableColumn<Row, String> memberCol = new TableColumn<>("Member Name");
        memberCol.setCellValueFactory(c -> c.getValue().member);
        memberCol.setPrefWidth(280);
        table.getColumns().add(numCol);
        table.getColumns().add(memberCol);

        ObservableList<Row> rows = FXCollections.observableArrayList();
        for (int i = 0; i < geneSet.getNumMembers(); i++) {
            rows.add(new Row(i + 1, geneSet.getMember(i)));
        }
        table.setItems(rows);

        VBox box = new VBox(4, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        root.setCenter(box);
    }

    private static final class Row {
        final SimpleStringProperty index;
        final SimpleStringProperty member;

        Row(int index, String member) {
            this.index = new SimpleStringProperty(Integer.toString(index));
            this.member = new SimpleStringProperty(member);
        }
    }

    @Override
    public String getTitle() {
        return geneSet.getName();
    }

    @Override
    public String getIconResourceId() {
        return "Grp.gif";
    }

    @Override
    public javafx.scene.Node getContent() {
        return root;
    }
}
