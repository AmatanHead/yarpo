#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import os
import sys

sys.path.append(os.getcwd())

from pywidgets import Application, Widget, Label, VBoxLayout, PushButton

app = Application()
assert "Application" == app.get_class_name()

main_window = Widget()
assert "Widget" == main_window.get_class_name()

layout = VBoxLayout(main_window)
assert "VBoxLayout" == layout.get_class_name()

main_window.set_layout(layout)

label = Label(main_window)
assert "Label" == label.get_class_name()

label.set_text("Привет из Python!")
layout.add_widget(label)

button = PushButton(main_window)
assert "PushButton" == button.get_class_name()

button.set_text("Кнопка не работает")
layout.add_widget(button)

main_window.set_window_title("Тест 4")
main_window.set_size(400, 300)
main_window.set_visible(True)

status = app.exec()
sys.exit(status)
