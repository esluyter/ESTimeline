ESFxSynthEditView : ESClipEditView {

  *new { |clip, timeline, template, index|
    var panelFont = Font.sansSerif(16);
    var defNameView, targetView, addActionView, argsView, funcView, doPlayFuncBox;
    var adjustBg = {
      funcView.visible_(doPlayFuncBox.value);
      defNameView.background_(if (doPlayFuncBox.value) { Color.gray(0.8) } { Color.white });
    };

    this.prNew(clip, timeline, {
      var func = funcView.string.interpret;
      if (func.isNil) {
        ESBulkEditWindow.ok
      } {
        clip.func = func;
      };
      clip.doPlayFunc = doPlayFuncBox.value;
      clip.name = nameField.string.asSymbol;
      clip.args = argsView.value;
      clip.defName = ("{" ++ defNameView.string ++ "}").interpret;

      argsView.free;
      argsView.init(clip.args.copy, clip.argControls);

      timeline.addUndoPoint;
    });

    editorWindow.name_("Insert FX");

    StaticText(editorWindow, Rect(40, 10, 180, 20)).string_("defName").font_(panelFont);
    StaticText(editorWindow, Rect(320, 10, 100, 20)).string_(". . . or:");
    defNameView = TextField(editorWindow, Rect(30, 30, 320, 40)).string_(clip.defName.asESDisplayString).font_(Font.monospace(14));

    StaticText(editorWindow, Rect(390, 10, 120, 20)).string_("doPlayFunc").font_(panelFont);
    doPlayFuncBox = CheckBox(editorWindow, Rect(370, 10, 20, 20)).value_(clip.doPlayFunc).action_(adjustBg);
    funcView = CodeView(editorWindow, Rect(370, 30, 430, 570)).font_(Font.monospace(12)).string_(clip.func.asCompileString);

    StaticText(editorWindow, Rect(170, 105, 180, 20)).string_("args").font_(panelFont);
    Button(editorWindow, Rect(145, 105, 20, 20)).states_([["⟳"]]).font_(Font.sansSerif(30)).action_({
      //argsView.freeArgControls;
      //argsView.initArgControls(SynthDescLib.at(if (doPlayFuncBox.value) { clip.autoDefName } { defNameView.string.interpret }).controls);
      argsView.free;
      argsView.init(clip.args.copy, clip.argControls);
    });
    argsView = ESFxArgsView(editorWindow, Rect(10, 130, 350, 460), clip, template, index);

    adjustBg.value;
  }
}

ESFxArgsView : ScrollView {
  var args, argControls, template, index;
  var argPairs, automatedPairs;
  var defaultArgs;

  var control_i;

  *new { |parent, bounds, clip, template, index|
    ^super.new(parent, bounds).hasBorder_(false).init(clip.args.copy, clip.argControls, template, index);
  }

  initArgControls { |argargControls|
    var i = control_i;
    argControls = argargControls ?? argControls;
    defaultArgs = [];
    argControls.do { |cn|
      if (args.indexOf(cn.name).isNil and: { try { template.envs.fx[index][cn.name].isNil } { true } }) {
        var thisIndex = defaultArgs.size;
        var action = { |view, x, y, mods, buttNum, clickCount|
          if (clickCount > 1) {
            args = args.add(cn.name).add(cn.defaultValue);
            this.free;
            this.init(args, argControls, template, index);
          };
        };
        defaultArgs = defaultArgs.add([
          StaticText(this, Rect(10, 2.5 + (i * 30), 140, 25)).string_(cn.name).align_(\right).font_(Font().italic_(true)).stringColor_(Color.gray(0.5)).mouseDownAction_(action).setContextMenuActions(
            MenuAction("Add automation envelope", {
              this.addEnvelope(cn.name, cn.defaultValue);
            })
          ),
          StaticText(this, Rect(160, 2.5 + (i * 30), this.bounds.width - 165, 25)).string_(cn.defaultValue).stringColor_(Color.gray(0.5)).mouseDownAction_(action).setContextMenuActions(
            MenuAction("Add automation envelope", {
              this.addEnvelope(cn.name, cn.defaultValue);
            })
          )
        ]);
        i = i + 1;
      };
    };
  }

  init { |argargs, argargControls, argtemplate, argindex|
    var i = 0;

    template = argtemplate ?? template;
    index = argindex ?? index;
    args = argargs ?? args;


    argPairs = [];
    automatedPairs = [];
    if (template.envs.fx[index].notNil) {
      template.envs.fx[index].keysValuesDo { |key, value|
        automatedPairs = automatedPairs.add([
          StaticText(this, Rect(10, 2.5 + (i * 30), 140, 25)).string_(key).font_(Font().bold_(true)).align_(\right).mouseDownAction_(action).setContextMenuActions(
            MenuAction("Remove automation envelope", {
              this.removeEnvelope(key);
            })
          ),
          StaticText(this, Rect(160, 2.5 + (i * 30), this.bounds.width - 165, 25)).string_("automated").font_(Font().italic_(true)).setContextMenuActions(
            MenuAction("Remove automation envelope", {
              this.removeEnvelope(key);
            })
          )
        ]);
        i = i + 1;
      };
    };
    args.pairsDo { |key, val, thisIndex|
      if (try { template.envs.fx[index][key].isNil } { true }) {
        var action = { |view, x, y, mods, buttNum, clickCount|
          if (clickCount > 1) {
            args.removeAt(thisIndex);
            args.removeAt(thisIndex);
            this.free;
            this.init(args, argControls, template, index);
          };
        };
        argPairs = argPairs.add([
          StaticText(this, Rect(10, 2.5 + (i * 30), 140, 25)).string_(key).align_(\right).mouseDownAction_(action).setContextMenuActions(
            MenuAction("Add automation envelope", {
              this.addEnvelope(key, val.value);
            })
          ),
          TextField(this, Rect(160, 2.5 + (i * 30), this.bounds.width - 165, 25)).string_(val.asESDisplayString).font_(Font.monospace(14)).keyUpAction_({ |view|
            args[thisIndex + 1] = ("{" ++ view.string ++ "}").interpret;
          }).setContextMenuActions(
            MenuAction("Add automation envelope", {
              this.addEnvelope(key, val.value);
            })
          )
        ]);
        i = i + 1;
      };
    };

    control_i = i;
    this.initArgControls(argargControls);
  }

  addEnvelope { |param, value|
    var min = 0, max = 1, isExponential = false;
    var arr = template.envs.fx;
    var clip;

    if (param.asSpec.notNil) {
      var spec = param.asSpec;
      min = spec.minval;
      max = spec.maxval;
      isExponential = (spec.warp.class == ExponentialWarp);
    };

    while { (arr.size - 1) < index } {
      arr = arr.add(());
    };

    clip = ESMixerChannelEnv(Env(), min(min, value), max(max, value), isExponential: isExponential);
    clip.env = Env(clip.prValueUnscale(value).dup(2), [0], [0]);
    arr[index][param] = clip;
    template.envs.fx = arr;

    this.free;
    this.init;
  }

  removeEnvelope { |param|
    template.envs.fx[index][param].stop; template.envs.fx[index][param] = nil;
    template.envs.fx = template.envs.fx;

    this.free;
    this.init;
  }

  freeArgControls {
    defaultArgs.flat.do(_.remove);
  }

  free {
    automatedPairs.flat.do(_.remove);
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