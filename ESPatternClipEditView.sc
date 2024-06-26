ESPatternClipEditView : ESClipEditView {
  *new { |clip, timeline|
    var panelFont = Font("Helvetica", 16);
    var codeView, sidePanel, nameField, startTimeView, durationView, offsetView, colorView, randSeedField, isSeededBox;

    if (editorWindow.notNil) { editorWindow.close };
    editorWindow = Window("Pattern Clip Editor", Rect(0, 0, 1000, 600))
    .background_(Color.gray(0.9))
    .front;

    codeView = CodeView(editorWindow, Rect(0, 0, 800, 600)).font_(Font.monospace(16)).string_(clip.patternString);

    if (timeline.useEnvir) {
      codeView.interpretEnvir_(timeline.envir);
    };

    sidePanel = View(editorWindow, Rect(810, 30, 180, 550));

    StaticText(sidePanel, Rect(0, 0, 180, 20)).string_("name").font_(panelFont);
    nameField = TextField(sidePanel, Rect(0, 20, 180, 20)).font_(Font.monospace(16)).string_(clip.name);
    StaticText(sidePanel, Rect(0, 50, 180, 20)).string_("startTime").font_(panelFont);
    startTimeView = NumberBox(sidePanel, Rect(0, 70, 180, 20)).font_(Font.monospace(16)).value_(clip.startTime);
    StaticText(sidePanel, Rect(0, 100, 180, 20)).string_("duration").font_(panelFont);
    durationView = NumberBox(sidePanel, Rect(0, 120, 180, 20)).font_(Font.monospace(16)).value_(clip.duration);
    StaticText(sidePanel, Rect(0, 150, 180, 20)).string_("offset").font_(panelFont);
    offsetView = NumberBox(sidePanel, Rect(0, 170, 180, 20)).font_(Font.monospace(16)).value_(clip.offset);
    StaticText(sidePanel, Rect(0, 200, 180, 20)).string_("color").font_(panelFont);
    colorView = UserView(sidePanel, Rect(0, 220, 180, 20)).drawFunc_({ |view|
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

    Button(sidePanel, Rect(0, 485, 180, 30)).string_("Cancel").font_(panelFont.copy.size_(14)).action_({ editorWindow.close });
    Button(sidePanel, Rect(0, 520, 180, 30)).string_("Save").font_(panelFont.copy.size_(14)).action_({
      clip.name = nameField.string.asSymbol;
      clip.pattern = ("{" ++ codeView.string ++ "}").interpret;
      clip.randSeed = randSeedField.string.asInteger;
      clip.isSeeded = isSeededBox.value;
      clip.color = colorView.background;
      clip.startTime = startTimeView.value;
      clip.duration =  durationView.value;
      clip.offset = offsetView.value;

      timeline.addUndoPoint;
    });
  }
}