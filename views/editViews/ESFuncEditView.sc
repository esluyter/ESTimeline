ESFuncEditView : ESClipEditView {
  *new { |timeline|
    var panelFont = Font("Helvetica", 16);
    var funcButton, cleanupFuncButton, funcView, cleanupFuncView, sidePanel, bootBox, envirBox, optimizeBox;

    if (editorWindow.notNil) { editorWindow.close };
    editorWindow = Window("Timeline Function Editor", Rect(Window.availableBounds.width - 1100, 0, 1000, 600))
    .background_(Color.gray(0.9))
    .front;

    funcButton = Button(editorWindow, Rect(668, 0, 100, 30)).states_(
      [["prepFunc", Color.gray(0.3), Color.gray(0.7)],
        ["prepFunc", Color.black, Color.white]])
    .value_(1).action_({
      funcButton.value_(1); cleanupFuncButton.value_(0);
      funcView.visible_(true); cleanupFuncView.visible_(false); })
    .font_(panelFont)
    .focusColor_(Color.clear);

    cleanupFuncButton = Button(editorWindow, Rect(770, 0, 120, 30)).states_(
      [["cleanupFunc", Color.gray(0.3), Color.gray(0.7)],
        ["cleanupFunc", Color.black, Color.white]])
    .value_(0).action_({
      funcButton.value_(0); cleanupFuncButton.value_(1);
      funcView.visible_(false); cleanupFuncView.visible_(true); })
    .font_(panelFont)
    .focusColor_(Color.clear);

    funcView = CodeView(editorWindow, Rect(0, 30, 900, 570)).font_(Font.monospace(16)).string_(timeline.prepFunc.asESDisplayString);
    cleanupFuncView = CodeView(editorWindow, Rect(0, 30, 900, 570)).font_(Font.monospace(16)).string_(timeline.cleanupFunc.asESDisplayString).visible_(false);

    if (timeline.useEnvir) {
      funcView.interpretEnvir_(timeline.envir);
      cleanupFuncView.interpretEnvir_(timeline.envir);
    };

    sidePanel = View(editorWindow, Rect(905, 30, 90, 550));

    StaticText(sidePanel, Rect(0, 0, 90, 20)).string_("bootOnPrep").font_(panelFont);
    bootBox = CheckBox(sidePanel, Rect(0, 15, 20, 20)).value_(timeline.bootOnPrep);
    StaticText(sidePanel, Rect(0, 35, 90, 120)).align_(\topLeft).string_("If this is checked, the prepFunc will be wrapped in a waitForBoot");

    StaticText(sidePanel, Rect(0, 140, 90, 20)).string_("useEnvir").font_(panelFont);
    envirBox = CheckBox(sidePanel, Rect(0, 155, 20, 20)).value_(timeline.useEnvir);
    StaticText(sidePanel, Rect(0, 175, 90, 200)).align_(\topLeft).string_("If this is checked, the timeline will use its own isolated Environment");

    StaticText(sidePanel, Rect(0, 295, 90, 20)).string_("optimizeView").font_(panelFont);
    optimizeBox = CheckBox(sidePanel, Rect(0, 310, 20, 20)).value_(timeline.optimizeView);
    StaticText(sidePanel, Rect(0, 330, 90, 200)).align_(\topLeft).string_("Check this for particularly heavy timelines");

    Button(sidePanel, Rect(0, 410, 90, 25)).string_("Open in IDE").action_({
      // open / load whichever view is currently visible
      if (funcView.visible) {
        Document.new("Edit Timeline Init Func", funcView.string).promptToSave_(false).front;
      } {
        Document.new("Edit Timeline Cleanup Func", cleanupFuncView.string).promptToSave_(false).front;
      };
    });
    Button(sidePanel, Rect(0, 440, 90, 25)).string_("Copy from IDE").action_({
      // copy to whichever currently visible
      if (funcView.visible) {
        funcView.string_(Document.current.string);
      } {
        cleanupFuncView.string_(Document.current.string);
      }
    });

    Button(sidePanel, Rect(0, 485, 90, 30)).string_("Cancel").font_(panelFont.copy.size_(14)).action_({ editorWindow.close });
    Button(sidePanel, Rect(0, 520, 90, 30)).string_("Save").font_(panelFont.copy.size_(14)).action_({
      var prepFunc = ("{" ++ funcView.string ++ "}").interpret;
      var cleanupFunc = ("{" ++ cleanupFuncView.string ++ "}").interpret;
      if ((prepFunc.isNil) or: (cleanupFunc.isNil)) {
        ESBulkEditWindow.ok
      } {
        timeline.prepFunc = prepFunc;
        timeline.cleanupFunc = cleanupFunc;
        timeline.bootOnPrep = bootBox.value;
        timeline.useEnvir = envirBox.value;

        timeline.init(cleanupFirst: true);
        timeline.addUndoPoint;
      };
    });
  }
}