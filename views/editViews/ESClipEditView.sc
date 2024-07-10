ESClipEditView {
  classvar <>editorWindow;
  classvar sidePanel, nameField, <startTimeView, <durationView, <offsetView, colorView;

  classvar <thisClip;

  *closeWindow {
    if (editorWindow.notNil) {
      editorWindow.close;
      editorWindow = nil;
    };
  }

  *title { ^"Clip Editor" }

  *prNew { |clip, timeline, saveAction|
    var panelFont = Font.sansSerif(16);

    thisClip = clip;

    if (editorWindow.notNil) {
      editorWindow.onClose_({});
      editorWindow.close;
    };
    editorWindow = Window(this.title, Rect(Window.availableBounds.width - 1100, 0, 1000, 600))
    .background_(Color.gray(0.9))
    .front
    .onClose_{ thisClip = nil };

    sidePanel = View(editorWindow, Rect(810, 10, 180, 570));

    if (clip.class != ESClip) {
      StaticText(sidePanel, Rect(0, 0, 180, 20)).string_("name").font_(panelFont);
      nameField = TextField(sidePanel, Rect(0, 20, 180, 20)).font_(Font.monospace(16)).string_(clip.name);
    };

    StaticText(sidePanel, Rect(0, 45, 180, 20)).string_("startTime").font_(panelFont);
    startTimeView = TextField(sidePanel, Rect(0, 65, 180, 20)).font_(Font.monospace(16)).string_(clip.startTime.asString);
    StaticText(sidePanel, Rect(0, 90, 180, 20)).string_("duration").font_(panelFont);
    durationView = TextField(sidePanel, Rect(0, 110, 180, 20)).font_(Font.monospace(16)).string_(clip.duration.asString);

    if (clip.prHasOffset) {
      StaticText(sidePanel, Rect(0, 135, 180, 20)).string_("offset").font_(panelFont);
      offsetView = TextField(sidePanel, Rect(0, 155, 180, 20)).font_(Font.monospace(16)).string_(clip.offset.asString);
    };

    StaticText(sidePanel, Rect(0, 180, 180, 20)).string_("color").font_(panelFont);
    colorView = UserView(sidePanel, Rect(0, 200, 180, 20)).drawFunc_({ |view|
      Pen.use {
        Pen.addRect(Rect(0, 0, view.bounds.width, view.bounds.height));
        Pen.color = Color.gray(0.6);
        Pen.stroke;
      };
    });

    Button(sidePanel, Rect(0, 505, 180, 30)).string_("Cancel").font_(panelFont.copy.size_(14)).action_({ editorWindow.close });
    Button(sidePanel, Rect(0, 540, 180, 30)).string_("Save").font_(panelFont.copy.size_(14)).action_({
      try {
        saveAction.value;
      } {
        ESBulkEditWindow.ok;
      };
    });

    if (clip.class == ESClip) {
      colorView.background_(clip.rawColor).setContextMenuActions(
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
    } {
      colorView.background_(clip.rawColor).setContextMenuActions(
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
    };
  }

  *new { |clip, timeline|
    var funcView;
    var panelFont = Font.sansSerif(16);

    this.prNew(clip, timeline, {
      var string = funcView.string;
      var lines = string.split($\n);
      clip.name = lines[0].asSymbol;
      clip.comment = lines[1..].join($\n);

      clip.color = colorView.background;
      clip.startTime = startTimeView.string.interpret;
      clip.duration =  durationView.string.interpret;

      timeline.addUndoPoint;
    });

    funcView = CodeView(editorWindow, Rect(0, 0, 800, 600)).font_(panelFont).string_(if (clip.name.isNil) { "" } { clip.name.asString ++ "\n" } ++ clip.comment);
    if (timeline.useEnvir) {
      funcView.interpretEnvir_(timeline.envir);
    };
  }
}