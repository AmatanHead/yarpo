#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import os
import sys

sys.path.append(os.getcwd())

from pywidgets._pywidgets import *

app = Application_New()
assert "Application" == Object_GetClassName(app)

main_window = Widget_New()
assert "Widget" == Object_GetClassName(main_window)

layout = VBoxLayout_New(main_window)
assert "VBoxLayout" == Object_GetClassName(layout)

Widget_SetLayout(main_window, layout)

label = Label_New(main_window)
assert "Label" == Object_GetClassName(label)

Label_SetText(label, "Привет из Python!")
Layout_AddWidget(layout, label)

button = PushButton_New(main_window)
assert "PushButton" == Object_GetClassName(button)

PushButton_SetText(button, "Кнопка не работает")
Layout_AddWidget(layout, button)

Widget_SetWindowTitle(main_window, "Тест 3")
Widget_SetSize(main_window, 400, 300)
Widget_SetVisible(main_window, True)

status = Application_Exec(app)

print(status)

sys.exit(status)
