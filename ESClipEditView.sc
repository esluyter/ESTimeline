ESClipEditView {
  classvar <>editorWindow;

  *closeWindow {
    if (editorWindow.notNil) {
      editorWindow.close;
      editorWindow = nil;
    };
  }
}