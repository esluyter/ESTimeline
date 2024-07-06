ESBulkEditWindow {
  classvar editorWindow;

  *keyValue { |title = "Set all selected Synth Clip parameter", keyLabel = "key", keyDefault = \freq, valLabel = "val", valDefault = 440, checkLabel = "hard coded", checkDefault = false, showExtraField = false, extraFieldLabel = "", extraFieldDefault = "", callback|
    var width = 600, height = 300;
    var left = ((Window.availableBounds.width - width) / 2);
    var top = Window.availableBounds.height - height - 200;
    var keyField, valField, checkBox, extraField;
    editorWindow !? { editorWindow.close };
    editorWindow = Window(title, Rect(left, top, width, height)).front;
    StaticText(editorWindow, Rect(100, 30, 500, 20)).string_(title).font_(Font.sansSerif(20));
    StaticText(editorWindow, Rect(0, 80, 90, 40)).align_(\right).string_(keyLabel).font_(Font.sansSerif(16));
    keyField = TextField(editorWindow, Rect(100, 80, 400, 40)).string_(keyDefault).font_(Font.monospace(16));
    StaticText(editorWindow, Rect(0, 130, 90, 40)).align_(\right).string_(valLabel).font_(Font.sansSerif(16));
    valField = TextField(editorWindow, Rect(100, 130, 400, 40)).string_(valDefault).font_(Font.monospace(16));

    checkBox = CheckBox(editorWindow, Rect(100, 180, 20, 20)).value_(checkDefault);
    StaticText(editorWindow, Rect(120, 180, 200, 20)).string_(checkLabel).font_(Font.sansSerif(16));

    StaticText(editorWindow, Rect(250, 180, 90, 20)).string_(extraFieldLabel).align_(\right).font_(Font.sansSerif(16));
    extraField = TextField(editorWindow, Rect(350, 175, 150, 30)).string_(extraFieldDefault).visible_(showExtraField);

    Button(editorWindow, Rect(100, 220, 197.5, 40)).string_("OK").font_(Font.sansSerif(14)).action_({
      //var key = keyField.string.asSymbol;
      //var val = ("{" ++ valField.string ++ "}").interpret;
      callback.(keyField.string, valField.string, checkBox.value, extraField.string);
      editorWindow.close;
    });
    Button(editorWindow, Rect(302.5, 220, 197.5, 40)).string_("Cancel").font_(Font.sansSerif(14)).action_({ editorWindow.close });
  }

  *value { |title = "Set all selected Synth Clip defName", valDefault = "'default'", callback|
    var width = 600, height = 250;
    var left = ((Window.availableBounds.width - width) / 2);
    var top = Window.availableBounds.height - height - 200;
    var keyField, valField;
    editorWindow !? { editorWindow.close };
    editorWindow = Window(title, Rect(left, top, width, height)).front;
    StaticText(editorWindow, Rect(100, 30, 400, 20)).string_(title).font_(Font.sansSerif(20));
    StaticText(editorWindow, Rect(0, 80, 90, 40)).align_(\right).string_("value").font_(Font.sansSerif(16));
    valField = TextField(editorWindow, Rect(100, 80, 400, 40)).string_(valDefault).font_(Font.monospace(16));
    Button(editorWindow, Rect(100, 150, 197.5, 40)).string_("OK").font_(Font.sansSerif(14)).action_({
      var val = ("{" ++ valField.string ++ "}").interpret;
      callback.(val);
      editorWindow.close;
    });
    Button(editorWindow, Rect(302.5, 150, 197.5, 40)).string_("Cancel").font_(Font.sansSerif(14)).action_({ editorWindow.close });
  }

  *menu { |title = "", menuLabel, items, selectedItem, checkLabel = "", checkDefault = false, callback|
    var width = 600, height = 300;
    var left = ((Window.availableBounds.width - width) / 2);
    var top = Window.availableBounds.height - height - 200;
    var menu, checkBox, extraField;
    editorWindow !? { editorWindow.close };
    editorWindow = Window(title, Rect(left, top, width, height)).front;

    StaticText(editorWindow, Rect(100, 30, 500, 20)).string_(title).font_(Font.sansSerif(20));
    StaticText(editorWindow, Rect(0, 80, 90, 40)).align_(\right).string_(menuLabel).font_(Font.sansSerif(16));
    menu = PopUpMenu(editorWindow, Rect(100, 80, 400, 40)).items_(items).font_(Font.monospace(16)).value_(selectedItem ?? 0);

    checkBox = CheckBox(editorWindow, Rect(100, 180, 20, 20)).value_(checkDefault);
    StaticText(editorWindow, Rect(120, 180, 200, 20)).string_(checkLabel).font_(Font.sansSerif(16));

    Button(editorWindow, Rect(100, 220, 197.5, 40)).string_("OK").font_(Font.sansSerif(14)).action_({
      //var key = keyField.string.asSymbol;
      //var val = ("{" ++ valField.string ++ "}").interpret;
      callback.(menu.item, checkBox.value);
      editorWindow.close;
    });
    Button(editorWindow, Rect(302.5, 220, 197.5, 40)).string_("Cancel").font_(Font.sansSerif(14)).action_({ editorWindow.close });
  }
}