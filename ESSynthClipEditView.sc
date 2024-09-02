ESSynthClipEditView : ESClipEditView {

  *new { |clip, timeline|
    var panelFont = Font("Helvetica", 16);
    var defNameView, targetView, addActionView, argsView, sidePanel, startTimeView, durationView, offsetView, colorView, randSeedField;

    if (editorWindow.notNil) { editorWindow.close };
    editorWindow = Window("Synth Clip Editor", Rect(0, 0, 800, 600))
    .background_(Color.gray(0.9))
    .front;

    StaticText(editorWindow, Rect(20, 30, 180, 20)).string_("defName").font_(panelFont);
    defNameView = TextField(editorWindow, Rect(10, 50, 590, 40)).string_(clip.defName.asESDisplayString).font_(Font.monospace(16));

    StaticText(editorWindow, Rect(20, 100, 180, 20)).string_("target").font_(panelFont);
    targetView = TextField(editorWindow, Rect(10, 120, 290, 40)).string_(clip.target.asESDisplayString).font_(Font.monospace(16));

    StaticText(editorWindow, Rect(315, 100, 180, 20)).string_("addAction").font_(panelFont);
    addActionView = TextField(editorWindow, Rect(305, 120, 295, 40)).string_(clip.addAction.asESDisplayString).font_(Font.monospace(16));

    StaticText(editorWindow, Rect(170, 175, 180, 20)).string_("args").font_(panelFont);
    Button(editorWindow, Rect(145, 175, 20, 20)).states_([["⟳"]]).font_(Font.sansSerif(30)).action_({
      argsView.freeArgControls;
      argsView.initArgControls(SynthDescLib.at(defNameView.string.interpret).controls);
    });
    argsView = ESArgsView(editorWindow, Rect(10, 200, 590, 400), clip);

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
      //clip.args = ("{" ++ codeView.string ++ "}").interpret;
      clip.args = argsView.value;
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


ESArgsView : ScrollView {
  var args, argControls;
  var argPairs;
  var defaultArgs;

  var control_i;

  *new { |parent, bounds, clip|
    ^super.new(parent, bounds).hasBorder_(false).init(clip.args, clip.argControls);
  }

  initArgControls { |argargControls|
    var i = control_i;
    argControls = argargControls;
    defaultArgs = [];
    argControls.do { |cn|
      if (args.indexOf(cn.name).isNil) {
        var index = defaultArgs.size;
        var action = { |view, x, y, mods, buttNum, clickCount|
          if (clickCount > 1) {
            args = args.add(cn.name).add(cn.defaultValue);
            this.free;
            this.init(args, argControls);
          };
        };
        defaultArgs = defaultArgs.add([
          StaticText(this, Rect(10, 2.5 + (i * 30), 140, 25)).string_(cn.name).align_(\right).font_(Font().italic_(true)).stringColor_(Color.gray(0.5)).mouseDownAction_(action),
          StaticText(this, Rect(160, 2.5 + (i * 30), this.bounds.width - 165, 25)).string_(cn.defaultValue).stringColor_(Color.gray(0.5)).mouseDownAction_(action)
        ]);
        i = i + 1;
      };
    };
  }

  init { |argargs, argargControls|
    var i = 0;
    args = argargs;
    argPairs = [];
    args.pairsDo { |key, val, index|
      var action = { |view, x, y, mods, buttNum, clickCount|
        if (clickCount > 1) {
          args.removeAt(index);
          args.removeAt(index);
          this.free;
          this.init(args, argControls);
        };
      };
      argPairs = argPairs.add([
        StaticText(this, Rect(10, 2.5 + (i * 30), 140, 25)).string_(key).align_(\right).mouseDownAction_(action),
        TextField(this, Rect(160, 2.5 + (i * 30), this.bounds.width - 165, 25)).string_(val.asESDisplayString).font_(Font.monospace(14))
      ]);
      i = i + 1;
    };

    control_i = i;
    this.initArgControls(argargControls);
  }

  freeArgControls {
    defaultArgs.flat.do(_.remove);
  }

  free {
    argPairs.flat.do(_.remove);
    this.freeArgControls;
  }

  value {
    var ret = [];
    argPairs.do { |pair|
      ret = ret.add(pair[0].string.asSymbol).add(("{" ++ pair[1].string ++ "}").interpret);
    };
    ^ret;
  }
}