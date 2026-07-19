/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Search / filter helpers for lists. */
public final class FxSearchField {

    private FxSearchField() {
    }

    /** Filter field above {@code listView}; returns the VBox to place in the UI. */
    public static <T> VBox wrapList(ListView<T> listView, Function<T, String> textOf) {
        ObservableList<T> source = listView.getItems();
        FilteredList<T> filtered = new FilteredList<>(source, t -> true);
        listView.setItems(filtered);

        TextField field = new TextField();
        field.setPromptText("Filter…");
        field.textProperty().addListener((obs, o, n) -> {
            String q = n == null ? "" : n.trim().toLowerCase(Locale.ROOT);
            filtered.setPredicate(item -> q.isEmpty()
                    || (item != null && containsIgnoreCase(textOf.apply(item), q)));
        });

        VBox box = new VBox(4, field, listView);
        VBox.setVgrow(listView, Priority.ALWAYS);
        return box;
    }

    /** Filter field that reports the trimmed lowercased query. */
    public static TextField bind(Consumer<String> onChange) {
        TextField field = new TextField();
        field.setPromptText("Filter…");
        field.textProperty().addListener((obs, o, n) ->
                onChange.accept(n == null ? "" : n.trim().toLowerCase(Locale.ROOT)));
        return field;
    }

    public static boolean matches(String haystack, String queryLower) {
        return queryLower == null || queryLower.isEmpty() || containsIgnoreCase(haystack, queryLower);
    }

    /** Replace list contents, preserving a {@link FilteredList} wrapper when present. */
    public static <T> void replaceItems(ListView<T> list, List<T> items) {
        ObservableList<T> current = list.getItems();
        if (current instanceof FilteredList) {
            @SuppressWarnings("unchecked")
            ObservableList<T> source = (ObservableList<T>) ((FilteredList<T>) current).getSource();
            source.setAll(items);
        } else {
            list.setItems(FXCollections.observableArrayList(items));
        }
    }

    private static boolean containsIgnoreCase(String haystack, String queryLower) {
        return haystack != null && haystack.toLowerCase(Locale.ROOT).contains(queryLower);
    }
}
