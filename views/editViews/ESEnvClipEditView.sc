ESEnvClipEditView : ESClipEditView {

  *new { |clip, timeline|
    var panelFont = Font("Helvetica", 16);
    var busView, makeBusBox, makeBusRateMenu, targetView, addActionView, codeView, minView, maxView, curveView, isExponentialBox;

    this.prNew(clip, timeline, {

      clip.env = ("{" ++ codeView.string ++ "}").interpret;
      clip.target = ("{" ++ targetView.string ++ "}").interpret;
      clip.addAction = ("{" ++ addActionView.string ++ "}").interpret;
      clip.color = colorView.background;
      clip.startTime = startTimeView.string.interpret;
      clip.duration =  durationView.string.interpret;
      clip.offset = offsetView.string.interpret;
      clip.min = minView.value;
      clip.max = maxView.value;
      clip.curve = curveView.value;
      clip.isExponential = isExponentialBox.value;

      clip.makeBusRate = makeBusRateMenu.item.asSymbol;
      clip.makeBus = makeBusBox.value;
      if (clip.makeBus.not) {
        clip.bus = ("{" ++ busView.string ++ "}").interpret;
      };
      clip.name = nameField.string.asSymbol;

      timeline.addUndoPoint;
    });

    StaticText(editorWindow, Rect(20, 30, 180, 20)).string_("bus").font_(panelFont);
    busView = TextField(editorWindow, Rect(10, 50, 350, 40)).string_(if (clip.makeBus.not) { clip.bus.asESDisplayString } { "" }).font_(Font.monospace(16));

    StaticText(editorWindow, Rect(365, 30, 100, 30)).string_(". . . or:");
    makeBusBox = CheckBox(editorWindow, Rect(415, 30, 20, 20)).value_(clip.makeBus);
    StaticText(editorWindow, Rect(440, 30, 200, 20)).string_("makeBus with rate:").font_(panelFont);
    makeBusRateMenu = PopUpMenu(editorWindow, Rect(415, 50, 180, 30)).items_(["audio", "control"]).value_([\audio, \control].indexOf(clip.makeBusRate));

    StaticText(editorWindow, Rect(20, 100, 180, 20)).string_("target").font_(panelFont);
    targetView = TextField(editorWindow, Rect(10, 120, 290, 40)).string_(clip.target.asESDisplayString).font_(Font.monospace(16));

    StaticText(editorWindow, Rect(315, 100, 180, 20)).string_("addAction").font_(panelFont);
    addActionView = TextField(editorWindow, Rect(305, 120, 290, 40)).string_(clip.addAction.asESDisplayString).font_(Font.monospace(16));

    StaticText(editorWindow, Rect(20, 175, 50, 20)).string_("env").font_(panelFont);
    StaticText(editorWindow, Rect(50, 177, 480, 20)).string_("... edit code if you must or use cmd-e for mouse breakpoint editor mode").font_(Font.sansSerif(13));
    codeView = CodeView(editorWindow, Rect(10, 200, 790, 400)).font_(Font.monospace(16)).string_(clip.env.asESDisplayString).background_(Color.gray(0.8));
    if (timeline.useEnvir) {
      codeView.interpretEnvir_(timeline.envir);
    };

    StaticText(sidePanel, Rect(0, 300, 180, 20)).string_("min").font_(panelFont);
    minView = NumberBox(sidePanel, Rect(0, 320, 180, 20)).font_(Font.monospace(16)).value_(clip.min);
    StaticText(sidePanel, Rect(0, 350, 180, 20)).string_("max").font_(panelFont);
    maxView = NumberBox(sidePanel, Rect(0, 370, 180, 20)).font_(Font.monospace(16)).value_(clip.max);
    StaticText(sidePanel, Rect(0, 400, 180, 20)).string_("curve").font_(panelFont);
    curveView = NumberBox(sidePanel, Rect(0, 420, 180, 20)).font_(Font.monospace(16)).value_(clip.curve);
    isExponentialBox = CheckBox(sidePanel, Rect(0, 450, 20, 20)).value_(clip.isExponential);
    StaticText(sidePanel, Rect(20, 450, 150, 20)).string_("isExponential").font_(panelFont);
  }
}