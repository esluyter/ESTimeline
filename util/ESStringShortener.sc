ESStringShortener {
  *trim { |string, width, font|
    while { max(0, width) < (QtGUI.stringBounds(string, font).width) } {
      if (string.size == 1) {
        string = "";
      } {
        string = string[0..string.size-2];
      };
    };
    ^string;
  }

  *trimWidth { |string, width, font|
    var newWidth;
    while { max(0, width) < (newWidth = QtGUI.stringBounds(string, font).width) } {
      if (string.size == 1) {
        string = "";
      } {
        string = string[0..string.size-2];
      };
    };
    ^[string, newWidth];
  }
}