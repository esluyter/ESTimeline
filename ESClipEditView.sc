ESClipEditView {
  classvar <>editorWindow;

  *closeWindow {
    if (editorWindow.notNil) {
      editorWindow.close;
      editorWindow = nil;
    };
  }

  *new { |clip, timeline|
    var panelFont = Font("Helvetica", 16);
    var funcView, sidePanel, startTimeView, durationView, colorView;

    if (editorWindow.notNil) { editorWindow.close };
    editorWindow = Window("Comment Editor", Rect(0, 0, 1000, 600))
    .background_(Color.gray(0.9))
    .front;

    funcView = CodeView(editorWindow, Rect(0, 0, 800, 600)).font_(panelFont).string_(clip.comment);
    if (timeline.useEnvir) {
      funcView.interpretEnvir_(timeline.envir);
    };

    sidePanel = View(editorWindow, Rect(810, 30, 180, 550));
    StaticText(sidePanel, Rect(0, 0, 180, 20)).string_("startTime").font_(panelFont);
    startTimeView = NumberBox(sidePanel, Rect(0, 20, 180, 20)).font_(Font.monospace(16)).value_(clip.startTime);
    StaticText(sidePanel, Rect(0, 50, 180, 20)).string_("duration").font_(panelFont);
    durationView = NumberBox(sidePanel, Rect(0, 70, 180, 20)).font_(Font.monospace(16)).value_(clip.duration);
    StaticText(sidePanel, Rect(0, 100, 180, 20)).string_("offset").font_(panelFont);

    StaticText(sidePanel, Rect(0, 150, 180, 20)).string_("color").font_(panelFont);
    colorView = UserView(sidePanel, Rect(0, 170, 180, 20)).drawFunc_({ |view|
      Pen.use {
        Pen.addRect(Rect(0, 0, view.bounds.width, view.bounds.height));
        Pen.color = Color.gray(0.6);
        Pen.stroke;
      };
    }).background_(clip.rawColor).setContextMenuActions(
      MenuAction("Red", { colorView.background = Color.hsv(0, 0.2, 0.9) }),
      MenuAction("Orange", { colorView.background = Color.hsv(0.07, 0.2, 0.9) }),
      MenuAction("Yellow", { colorView.background = Color.hsv(0.14, 0.2, 0.9) }),
      MenuAction("Lime", { colorView.background = Color.hsv(0.23, 0.2, 0.9) }),
      MenuAction("Green", { colorView.background = Color.hsv(0.3, 0.3, 0.85) }),
      MenuAction("Teal", { colorView.background = Color.hsv(0.48, 0.3, 0.85) }),
      MenuAction("Cyan", { colorView.background = Color.hsv(0.52, 0.2, 0.9) }),
      MenuAction("Blue", { colorView.background = Color.hsv(0.6, 0.25, 0.85) }),
      MenuAction("Purple", { colorView.background = Color.hsv(0.72, 0.2, 0.9) }),
      MenuAction("Magenta", { colorView.background = Color.hsv(0.82, 0.3, 0.9) }),
      MenuAction("Pink", { colorView.background = Color.hsv(0.9, 0.1, 0.9) }),
      MenuAction("[default]", { colorView.background = nil }),
      MenuAction(""),
      MenuAction("Lighten", { colorView.background = Color.white.lighten(colorView.background, 0.1) }),
      MenuAction("Darken", { colorView.background = Color.black.darken(colorView.background, 0.1) }),
      MenuAction("Saturate", { colorView.background = Color.red.saturationBlend(colorView.background, 0.8) }),
      MenuAction("Desaturate", { colorView.background = Color.black.saturationBlend(colorView.background, 0.8) }),
    );

    Button(sidePanel, Rect(0, 485, 180, 30)).string_("Cancel").font_(panelFont.copy.size_(14)).action_({ editorWindow.close });
    Button(sidePanel, Rect(0, 520, 180, 30)).string_("Save").font_(panelFont.copy.size_(14)).action_({
      clip.comment = funcView.string;

      clip.color = colorView.background;
      clip.startTime = startTimeView.value;
      clip.duration =  durationView.value;

      timeline.addUndoPoint;
    });
  }
}