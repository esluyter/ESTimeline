ESSynthClipEditView : ESClipEditView {

  *new { |clip, timeline|
    var panelFont = Font.sansSerif(16);
    var defNameView, targetView, addActionView, argsView, funcView;

    this.prNew(clip, timeline, {
      clip.name = nameField.string.asSymbol;

      clip.args = argsView.value;
      clip.defName = ("{" ++ defNameView.string ++ "}").interpret;
      clip.target = ("{" ++ targetView.string ++ "}").interpret;
      clip.addAction = ("{" ++ addActionView.string ++ "}").interpret;
      clip.color = colorView.background;
      clip.startTime = startTimeView.string.interpret;
      clip.duration =  durationView.string.interpret;

      timeline.addUndoPoint;
    });

    funcView = CodeView(editorWindow, Rect(370, 30, 430, 570)).font_(Font.monospace(12)).string_("");

    StaticText(editorWindow, Rect(20, 30, 180, 20)).string_("defName").font_(panelFont);
    StaticText(editorWindow, Rect(320, 30, 100, 20)).string_(". . . or:");
    defNameView = TextField(editorWindow, Rect(10, 50, 320, 40)).string_(clip.defName.asESDisplayString).font_(Font.monospace(14));

    StaticText(editorWindow, Rect(20, 100, 180, 20)).string_("target").font_(panelFont);
    targetView = TextField(editorWindow, Rect(10, 120, 170, 40)).string_(clip.target.asESDisplayString).font_(Font.monospace(14));

    StaticText(editorWindow, Rect(195, 100, 180, 20)).string_("addAction").font_(panelFont);
    addActionView = TextField(editorWindow, Rect(185, 120, 175, 40)).string_(clip.addAction.asESDisplayString).font_(Font.monospace(14));

    StaticText(editorWindow, Rect(170, 175, 180, 20)).string_("args").font_(panelFont);
    Button(editorWindow, Rect(145, 175, 20, 20)).states_([["⟳"]]).font_(Font.sansSerif(30)).action_({
      argsView.freeArgControls;
      argsView.initArgControls(SynthDescLib.at(defNameView.string.interpret).controls);
    });
    argsView = ESArgsView(editorWindow, Rect(10, 200, 350, 400), clip);
  }
}


ESArgsView : ScrollView {
  var args, argControls;
  var argPairs;
  var defaultArgs;

  var control_i;

  *new { |parent, bounds, clip|
    ^super.new(parent, bounds).hasBorder_(false).init(clip.args.copy, clip.argControls);
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
        TextField(this, Rect(160, 2.5 + (i * 30), this.bounds.width - 165, 25)).string_(val.asESDisplayString).font_(Font.monospace(14)).keyUpAction_({ |view|
          args[index + 1] = ("{" ++ view.string ++ "}").interpret;
        })
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