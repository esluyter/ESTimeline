ESSynthClipEditView : ESClipEditView {
  *new { |clip, timeline|
    var panelFont = Font("Helvetica", 16);
    var defNameView, targetView, addActionView, codeView, sidePanel, startTimeView, durationView, offsetView, colorView, randSeedField;

    if (editorWindow.notNil) { editorWindow.close };
    editorWindow = Window("Synth Clip Editor", Rect(0, 0, 800, 600))
    .background_(Color.gray(0.9))
    .front;

    StaticText(editorWindow, Rect(20, 30, 180, 20)).string_("defName").font_(panelFont);
    defNameView = TextField(editorWindow, Rect(10, 50, 590, 40)).string_(clip.defNameString).font_(Font.monospace(16));

    StaticText(editorWindow, Rect(20, 100, 180, 20)).string_("target").font_(panelFont);
    targetView = TextField(editorWindow, Rect(10, 120, 290, 40)).string_(clip.targetString).font_(Font.monospace(16));

    StaticText(editorWindow, Rect(315, 100, 180, 20)).string_("addAction").font_(panelFont);
    addActionView = TextField(editorWindow, Rect(305, 120, 290, 40)).string_(clip.addActionString).font_(Font.monospace(16));

    StaticText(editorWindow, Rect(20, 175, 180, 20)).string_("args").font_(panelFont);
    codeView = CodeView(editorWindow, Rect(10, 200, 590, 400)).font_(Font.monospace(16)).string_(clip.argsString);

    sidePanel = View(editorWindow, Rect(610, 30, 180, 550));
    StaticText(sidePanel, Rect(0, 0, 180, 20)).string_("startTime").font_(panelFont);
    startTimeView = NumberBox(sidePanel, Rect(0, 20, 180, 20)).font_(Font.monospace(16)).value_(clip.startTime);
    StaticText(sidePanel, Rect(0, 50, 180, 20)).string_("duration").font_(panelFont);
    durationView = NumberBox(sidePanel, Rect(0, 70, 180, 20)).font_(Font.monospace(16)).value_(clip.duration);
    /* not yet relevant for synth clips
    StaticText(sidePanel, Rect(0, 100, 180, 20)).string_("offset").font_(panelFont);
    offsetView = NumberBox(sidePanel, Rect(0, 120, 180, 20)).font_(Font.monospace(16)).value_(clip.offset);
    */
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

    Button(sidePanel, Rect(0, 485, 180, 30)).string_("Cancel").font_(panelFont.copy.size_(14)).action_({ editorWindow.close });
    Button(sidePanel, Rect(0, 520, 180, 30)).string_("Save").font_(panelFont.copy.size_(14)).action_({
      clip.args = ("{" ++ codeView.string ++ "}").interpret;
      clip.defName = ("{" ++ defNameView.string ++ "}").interpret;
      clip.target = ("{" ++ targetView.string ++ "}").interpret;
      clip.addAction = ("{" ++ addActionView.string ++ "}").interpret;
      clip.color = colorView.background;
      clip.startTime = startTimeView.value;
      clip.duration =  durationView.value;
      clip.offset = offsetView.value;

      timeline.addUndoPoint;
    });
  }
}