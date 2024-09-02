ESFxSynthEditView : ESClipEditView {

  *new { |clip, timeline|
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

      timeline.addUndoPoint;
    });

    StaticText(editorWindow, Rect(40, 10, 180, 20)).string_("defName").font_(panelFont);
    StaticText(editorWindow, Rect(320, 10, 100, 20)).string_(". . . or:");
    defNameView = TextField(editorWindow, Rect(30, 30, 320, 40)).string_(clip.defName.asESDisplayString).font_(Font.monospace(14));

    StaticText(editorWindow, Rect(390, 10, 120, 20)).string_("doPlayFunc").font_(panelFont);
    doPlayFuncBox = CheckBox(editorWindow, Rect(370, 10, 20, 20)).value_(clip.doPlayFunc).action_(adjustBg);
    funcView = CodeView(editorWindow, Rect(370, 30, 430, 570)).font_(Font.monospace(12)).string_(clip.func.asCompileString);

    StaticText(editorWindow, Rect(170, 165, 180, 20)).string_("args").font_(panelFont);
    Button(editorWindow, Rect(145, 165, 20, 20)).states_([["⟳"]]).font_(Font.sansSerif(30)).action_({
      argsView.freeArgControls;
      argsView.initArgControls(SynthDescLib.at(if (doPlayFuncBox.value) { clip.autoDefName } { defNameView.string.interpret }).controls);
    });
    argsView = ESArgsView(editorWindow, Rect(10, 190, 350, 410), clip);

    adjustBg.value;
  }
}