ESFuncEditView : ESClipEditView {
  *new { |timeline|
    var panelFont = Font("Helvetica", 16);
    var funcButton, cleanupFuncButton, funcView, cleanupFuncView, sidePanel, bootBox;

    if (editorWindow.notNil) { editorWindow.close };
    editorWindow = Window("Timeline Function Editor", Rect(0, 0, 1000, 600))
    .background_(Color.gray(0.9))
    .front;

    funcButton = Button(editorWindow, Rect(0, 0, 100, 30)).states_([["initFunc"], ["initFunc", Color.white, Color.gray(0.6)]]).value_(1).action_({ funcButton.value_(1); cleanupFuncButton.value_(0); funcView.visible_(true); cleanupFuncView.visible_(false); });
    cleanupFuncButton = Button(editorWindow, Rect(100, 0, 100, 30)).states_([["cleanupFunc"], ["cleanupFunc", Color.white, Color.gray(0.6)]]).value_(0).action_({ funcButton.value_(0); cleanupFuncButton.value_(1); funcView.visible_(false); cleanupFuncView.visible_(true); });

    funcView = CodeView(editorWindow, Rect(0, 30, 900, 570)).font_(Font.monospace(16)).string_(timeline.initFuncString);
    cleanupFuncView = CodeView(editorWindow, Rect(0, 30, 900, 570)).font_(Font.monospace(16)).string_(timeline.cleanupFuncString).visible_(false);

    sidePanel = View(editorWindow, Rect(905, 30, 90, 550));

    StaticText(sidePanel, Rect(0, 0, 90, 20)).string_("bootOnInit").font_(panelFont);
    bootBox = CheckBox(sidePanel, Rect(0, 15, 20, 20)).value_(timeline.bootOnInit);
    StaticText(sidePanel, Rect(0, 35, 90, 200)).align_(\topLeft).string_("If this is checked, the initFunc will be wrapped in a waitForBoot");

    Button(sidePanel, Rect(0, 485, 90, 30)).string_("Cancel").font_(panelFont.copy.size_(14)).action_({ editorWindow.close });
    Button(sidePanel, Rect(0, 520, 90, 30)).string_("Save").font_(panelFont.copy.size_(14)).action_({
      timeline.initFunc = ("{" ++ funcView.string ++ "}").interpret;
      timeline.cleanupFunc = ("{" ++ cleanupFuncView.string ++ "}").interpret;
      timeline.bootOnInit = bootBox.value;

      timeline.init(cleanupFirst: true);
      timeline.addUndoPoint;
    });
  }
}