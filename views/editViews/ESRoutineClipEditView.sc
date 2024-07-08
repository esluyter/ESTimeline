ESRoutineClipEditView : ESClipEditView {
  *new { |clip, timeline|
    var panelFont = Font.sansSerif(16);
    var funcButton, stopFuncButton, funcView, stopFuncView, randSeedField, isSeededBox, addLatencyBox, fastForwardMenu;

    this.prNew(clip, timeline, {
      clip.name = nameField.string.asSymbol;
      clip.func = ("{" ++ funcView.string ++ "}").interpret;
      clip.stopFunc = ("{" ++ stopFuncView.string ++ "}").interpret;
      clip.randSeed = randSeedField.string.asInteger;
      clip.isSeeded = isSeededBox.value;
      clip.addLatency = addLatencyBox.value;
      clip.color = colorView.background;
      clip.startTime = startTimeView.value;
      clip.duration =  durationView.value;
      clip.offset = offsetView.value;
      clip.fastForward = fastForwardMenu.value;

      timeline.addUndoPoint;
    });


    funcButton = Button(editorWindow, Rect(568, 0, 100, 30)).states_(
      [["func", Color.gray(0.3), Color.gray(0.7)],
        ["func", Color.black, Color.white]])
    .value_(1).action_({
      funcButton.value_(1); stopFuncButton.value_(0);
      funcView.visible_(true); stopFuncView.visible_(false); })
    .font_(panelFont)
    .focusColor_(Color.clear);

    stopFuncButton = Button(editorWindow, Rect(670, 0, 120, 30)).states_(
      [["stopFunc", Color.gray(0.3), Color.gray(0.7)],
        ["stopFunc", Color.black, Color.white]])
    .value_(0).action_({
      funcButton.value_(0); stopFuncButton.value_(1);
      funcView.visible_(false); stopFuncView.visible_(true); })
    .font_(panelFont)
    .focusColor_(Color.clear);

    funcView = CodeView(editorWindow, Rect(0, 30, 800, 570)).font_(Font.monospace(16)).string_(clip.func.asESDisplayString);
    stopFuncView = CodeView(editorWindow, Rect(0, 30, 800, 570)).font_(Font.monospace(16)).string_(clip.stopFunc.asESDisplayString).visible_(false);

    if (timeline.useEnvir) {
      funcView.interpretEnvir_(timeline.envir);
      stopFuncView.interpretEnvir_(timeline.envir);
    };


    StaticText(sidePanel, Rect(0, 230, 180, 20)).string_("randSeed").font_(panelFont);
    randSeedField = TextField(sidePanel, Rect(0, 250, 180, 20)).font_(Font.monospace(16)).value_(clip.randSeed);
    isSeededBox = CheckBox(sidePanel, Rect(0, 280, 20, 20)).value_(clip.isSeeded);
    StaticText(sidePanel, Rect(20, 280, 150, 20)).string_("isSeeded").font_(panelFont);
    Button(sidePanel, Rect(100, 275, 80, 25)).string_("Re-roll").action_({ randSeedField.string_(rand(2000000000)) });

    StaticText(sidePanel, Rect(0, 320, 180, 20)).string_("when playing from middle:").font_(panelFont.copy.size_(14));
    fastForwardMenu = PopUpMenu(sidePanel, Rect(0, 340, 180, 30)).items_(["Don't play", "Fast forward", "Play from beginning"]).value_(clip.fastForward);

    addLatencyBox = CheckBox(sidePanel, Rect(0, 390, 20, 20)).value_(clip.addLatency);
    StaticText(sidePanel, Rect(20, 390, 150, 20)).string_("addLatency").font_(panelFont);

    Button(sidePanel, Rect(0, 430, 180, 25)).string_("Open in IDE").action_({
      // open / load whichever view is currently visible
      if (funcView.visible) {
        Document.new("Edit Routine Clip Func", funcView.string).promptToSave_(false).front;
      } {
        Document.new("Edit Routine Clip Cleanup Func", stopFuncView.string).promptToSave_(false).front;
      };
    });
    Button(sidePanel, Rect(0, 460, 180, 25)).string_("Copy from IDE").action_({
      // copy to whichever currently visible
      if (funcView.visible) {
        funcView.string_(Document.current.string);
      } {
        stopFuncView.string_(Document.current.string);
      }
    });
  }
}