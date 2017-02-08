# noinspection PyUnresolvedReferences
from pywidgets._pywidgets import (
    Object_GetClassName,
    Object_Delete,
    Application_New,
    Application_Exec,
    VBoxLayout_New,
    Layout_AddWidget,
    Widget_New,
    Widget_SetVisible,
    Widget_SetWindowTitle,
    Widget_SetLayout,
    Widget_SetSize,
    Label_New,
    Label_SetText,
    PushButton_New,
    PushButton_SetText,
    PushButton_SetOnClicked
)


__all__ = [
    'Application',
    'Widget',
    'Label',
    'VBoxLayout',
    'PushButton',
]


class PyWidgetBase(object):
    _ptr = None

    #: Once a widget was added to layout, it is considered bound.
    #: QT automatically frees all such objects so we don't need to call destructor.
    _is_bound = False

    def get_class_name(self):
        if self._ptr:
            return Object_GetClassName(self._ptr)

    def bind(self):
        if self._is_bound:
            raise RuntimeError('already bound')
        self._is_bound = True

    def __del__(self):
        if not self._is_bound and self._ptr:
            Object_Delete(self._ptr)


class Application(PyWidgetBase):
    def __init__(self):
        self._ptr = Application_New()

    def exec(self):
        return Application_Exec(self._ptr)


class Widget(PyWidgetBase):
    def __init__(self, parent=None):
        if parent is not None:
            assert isinstance(parent, PyWidgetBase)
            parent = parent._ptr
        else:
            parent = 0

        self._ptr = Widget_New(parent)

    def set_layout(self, layout):
        assert isinstance(layout, Layout)
        layout.bind()
        Widget_SetLayout(self._ptr, layout._ptr)

    def set_window_title(self, title=''):
        Widget_SetWindowTitle(self._ptr, title)

    def set_size(self, w, h):
        Widget_SetSize(self._ptr, w, h)

    def set_visible(self, visible=True):
        Widget_SetVisible(self._ptr, visible)


class Label(Widget):
    def __init__(self, parent=None):
        if parent is not None:
            assert isinstance(parent, PyWidgetBase)
            parent = parent._ptr
        else:
            parent = 0

        self._ptr = Label_New(parent)

    def set_text(self, text=''):
        Label_SetText(self._ptr, text)


class PushButton(Widget):
    def __init__(self, parent=None):
        if parent is not None:
            assert isinstance(parent, PyWidgetBase)
            parent = parent._ptr
        else:
            parent = 0

        self._callbacks = []

        self._ptr = PushButton_New(parent)
        PushButton_SetOnClicked(self._ptr, self._on_clicked)

    def set_text(self, text=''):
        PushButton_SetText(self._ptr, text)

    def set_on_clicked(self, callback):
        self._callbacks.append(callback)

    def _on_clicked(self):
        for callback in self._callbacks:
            callback(self)


class Layout(PyWidgetBase):
    def add_widget(self, widget):
        assert isinstance(widget, Widget)
        widget.bind()
        Layout_AddWidget(self._ptr, widget._ptr)


class VBoxLayout(Layout):
    def __init__(self, parent=None):
        if parent is not None:
            assert isinstance(parent, PyWidgetBase)
            parent = parent._ptr
        else:
            parent = 0

        self._ptr = VBoxLayout_New(parent)
