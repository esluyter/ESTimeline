ESBulkEditWindow {
  classvar editorWindow;

  *keyValue { |title = "Set all selected Synth Clip parameter" keyDefault = \freq, valDefault = 440, callback|
    var width = 600, height = 300;
    var left = ((Window.availableBounds.width - width) / 2);
    var top = Window.availableBounds.height - height - 200;
    var keyField, valField;
    editorWindow !? { editorWindow.close };
    editorWindow = Window(title, Rect(left, top, width, height)).front;
    StaticText(editorWindow, Rect(100, 30, 400, 20)).string_(title).font_(Font.sansSerif(20));
    StaticText(editorWindow, Rect(0, 80, 90, 40)).align_(\right).string_("key").font_(Font.sansSerif(16));
    keyField = TextField(editorWindow, Rect(100, 80, 400, 40)).string_(keyDefault).font_(Font.monospace(16));
    StaticText(editorWindow, Rect(0, 130, 90, 40)).align_(\right).string_("value").font_(Font.sansSerif(16));
    valField = TextField(editorWindow, Rect(100, 130, 400, 40)).string_(valDefault).font_(Font.monospace(16));
    Button(editorWindow, Rect(100, 200, 197.5, 40)).string_("OK").font_(Font.sansSerif(14)).action_({
      var key = keyField.string.asSymbol;
      var val = ("{" ++ valField.string ++ "}").interpret;
      callback.(key, val);
      editorWindow.close;
    });
    Button(editorWindow, Rect(302.5, 200, 197.5, 40)).string_("Cancel").font_(Font.sansSerif(14)).action_({ editorWindow.close });
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
}