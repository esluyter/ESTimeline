ESPatternClipEditView : ESClipEditView {
  *new { |clip, timeline|
    var panelFont = Font("Helvetica", 16);
    var codeView, randSeedField, isSeededBox;

    this.prNew(clip, timeline, {
      var pattern = ("{" ++ codeView.string ++ "}").interpret;
      if (pattern.isNil) {
        ESBulkEditWindow.ok;
      } {
        clip.name = nameField.string.asSymbol;
        clip.pattern = pattern;
        clip.randSeed = randSeedField.string.asInteger;
        clip.isSeeded = isSeededBox.value;
        clip.color = colorView.background;
        clip.startTime = startTimeView.string.interpret;
        clip.duration =  durationView.string.interpret;
        clip.offset = offsetView.string.interpret;

        timeline.addUndoPoint;
      };
    });

    codeView = CodeView(editorWindow, Rect(0, 0, 800, 600)).font_(Font.monospace(16)).string_(clip.patternString);

    if (timeline.useEnvir) {
      codeView.interpretEnvir_(timeline.envir);
    };

    StaticText(sidePanel, Rect(0, 250, 180, 20)).string_("randSeed").font_(panelFont);
    randSeedField = TextField(sidePanel, Rect(0, 270, 180, 20)).font_(Font.monospace(16)).value_(clip.randSeed);
    isSeededBox = CheckBox(sidePanel, Rect(0, 300, 20, 20)).value_(clip.isSeeded);
    StaticText(sidePanel, Rect(20, 300, 150, 20)).string_("isSeeded").font_(panelFont);
    Button(sidePanel, Rect(100, 295, 80, 25)).string_("Re-roll").action_({ randSeedField.string_(rand(2000000000)) });

    Button(sidePanel, Rect(0, 410, 180, 25)).string_("Open in IDE").action_({
      Document.new("Edit Pattern Clip", codeView.string).promptToSave_(false).front;
    });
    Button(sidePanel, Rect(0, 440, 180, 25)).string_("Copy from IDE").action_({
      codeView.string_(Document.current.string);
    });
  }
}