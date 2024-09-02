ESRoutineClipEditView : ESClipEditView {
  *new { |clip, timeline|
    var panelFont = Font("Helvetica", 16);
    var funcButton, cleanupFuncButton, funcView, cleanupFuncView, sidePanel, startTimeView, durationView, offsetView, colorView, randSeedField, isSeededBox, addLatencyBox, fastForwardMenu;

    if (editorWindow.notNil) { editorWindow.close };
    editorWindow = Window("Routine Clip Editor", Rect(0, 0, 1000, 600))
    .background_(Color.gray(0.9))
    .front;


    funcButton = Button(editorWindow, Rect(568, 0, 100, 30)).states_(
      [["func", Color.gray(0.3), Color.gray(0.7)],
        ["func", Color.black, Color.white]])
    .value_(1).action_({
      funcButton.value_(1); cleanupFuncButton.value_(0);
      funcView.visible_(true); cleanupFuncView.visible_(false); })
    .font_(panelFont)
    .focusColor_(Color.clear);

    cleanupFuncButton = Button(editorWindow, Rect(670, 0, 120, 30)).states_(
      [["cleanupFunc", Color.gray(0.3), Color.gray(0.7)],
        ["cleanupFunc", Color.black, Color.white]])
    .value_(0).action_({
      funcButton.value_(0); cleanupFuncButton.value_(1);
      funcView.visible_(false); cleanupFuncView.visible_(true); })
    .font_(panelFont)
    .focusColor_(Color.clear);

    funcView = CodeView(editorWindow, Rect(0, 30, 800, 570)).font_(Font.monospace(16)).string_(clip.funcString);
    cleanupFuncView = CodeView(editorWindow, Rect(0, 30, 800, 570)).font_(Font.monospace(16)).string_(clip.cleanupFuncString).visible_(false);

    if (timeline.useEnvir) {
      funcView.interpretEnvir_(timeline.envir);
      cleanupFuncView.interpretEnvir_(timeline.envir);
    };

    sidePanel = View(editorWindow, Rect(810, 30, 180, 550));
    StaticText(sidePanel, Rect(0, 0, 180, 20)).string_("startTime").font_(panelFont);
    startTimeView = NumberBox(sidePanel, Rect(0, 20, 180, 20)).font_(Font.monospace(16)).value_(clip.startTime);
    StaticText(sidePanel, Rect(0, 50, 180, 20)).string_("duration").font_(panelFont);
    durationView = NumberBox(sidePanel, Rect(0, 70, 180, 20)).font_(Font.monospace(16)).value_(clip.duration);
    StaticText(sidePanel, Rect(0, 100, 180, 20)).string_("offset").font_(panelFont);
    offsetView = NumberBox(sidePanel, Rect(0, 120, 180, 20)).font_(Font.monospace(16)).value_(clip.offset);
    StaticText(sidePanel, Rect(0, 150, 180, 20)).string_("color").font_(panelFont);
    colorView = UserView(sidePanel, Rect(0, 170, 180, 20)).drawFunc_({ |view|
      Pen.use {
        Pen.addRect(Rect(0, 0, view.bounds.width, view.bounds.height));
        Pen.color = Color.gray(0.6);
        Pen.stroke;
      };
    }).background_(clip.rawColor).setContextMenuActions(
      MenuAction("Red", { colorView.background = Color.hsv(0, 0.54, 0.7) }),
      MenuAction("Orange", { colorView.background = Color.hsv(0.07, 0.6, 0.7) }),
      MenuAction("Yellow", { colorView.background = Color.hsv(0.14, 0.55, 0.75) }),
      MenuAction("Lime", { colorView.background = Color.hsv(0.23, 0.5, 0.75) }),
      MenuAction("Green", { colorView.background = Color.hsv(0.3, 0.5, 0.6) }),
      MenuAction("Teal", { colorView.background = Color.hsv(0.48, 0.5, 0.5) }),
      MenuAction("Cyan", { colorView.background = Color.hsv(0.52, 0.5, 0.7) }),
      MenuAction("Blue", { colorView.background = Color.hsv(0.6, 0.7, 0.7) }),
      MenuAction("Purple", { colorView.background = Color.hsv(0.72, 0.5, 0.7) }),
      MenuAction("Magenta", { colorView.background = Color.hsv(0.82, 0.45, 0.7) }),
      MenuAction("Pink", { colorView.background = Color.hsv(0.9, 0.3, 0.85) }),
      MenuAction("[default]", { colorView.background = nil }),
      MenuAction(""),
      MenuAction("Lighten", { colorView.background = Color.white.lighten(colorView.background, 0.1) }),
      MenuAction("Darken", { colorView.background = Color.black.darken(colorView.background, 0.1) }),
      MenuAction("Saturate", { colorView.background = Color.red.saturationBlend(colorView.background, 0.8) }),
      MenuAction("Desaturate", { colorView.background = Color.black.saturationBlend(colorView.background, 0.8) }),
    );
    StaticText(sidePanel, Rect(0, 200, 180, 20)).string_("randSeed").font_(panelFont);
    randSeedField = TextField(sidePanel, Rect(0, 220, 180, 20)).font_(Font.monospace(16)).value_(clip.randSeed);
    isSeededBox = CheckBox(sidePanel, Rect(0, 250, 20, 20)).value_(clip.isSeeded);
    StaticText(sidePanel, Rect(20, 250, 150, 20)).string_("isSeeded").font_(panelFont);
    Button(sidePanel, Rect(100, 245, 80, 25)).string_("Re-roll").action_({ randSeedField.string_(rand(2000000000)) });

    StaticText(sidePanel, Rect(0, 290, 180, 20)).string_("when playing from middle:").font_(panelFont.copy.size_(14));
    fastForwardMenu = PopUpMenu(sidePanel, Rect(0, 310, 180, 30)).items_(["Don't play", "Fast forward", "Play from beginning"]).value_(clip.fastForward);

    addLatencyBox = CheckBox(sidePanel, Rect(0, 360, 20, 20)).value_(clip.addLatency);
    StaticText(sidePanel, Rect(20, 360, 150, 20)).string_("addLatency").font_(panelFont);

    Button(sidePanel, Rect(0, 410, 180, 25)).string_("Open in IDE").action_({
      // open / load whichever view is currently visible
      if (funcView.visible) {
        Document.new("Edit Routine Clip Func", funcView.string).promptToSave_(false).front;
      } {
        Document.new("Edit Routine Clip Cleanup Func", cleanupFuncView.string).promptToSave_(false).front;
      };
    });
    Button(sidePanel, Rect(0, 440, 180, 25)).string_("Copy from IDE").action_({
      // copy to whichever currently visible
      if (funcView.visible) {
        funcView.string_(Document.current.string);
      } {
        cleanupFuncView.string_(Document.current.string);
      }
    });

    Button(sidePanel, Rect(0, 485, 180, 30)).string_("Cancel").font_(panelFont.copy.size_(14)).action_({ editorWindow.close });
    Button(sidePanel, Rect(0, 520, 180, 30)).string_("Save").font_(panelFont.copy.size_(14)).action_({
      clip.func = ("{" ++ funcView.string ++ "}").interpret;
      clip.cleanupFunc = ("{" ++ cleanupFuncView.string ++ "}").interpret;
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
  }
}