//
// Created by Vladimir Goncharov on 02.02.17.
//

extern "C" {
#include <Python.h>
}

#include <QtWidgets>

#include "widgets.h"
#include "widgets_internal.h"


static PyObject* Py_Object_GetClassName(PyObject *self, PyObject *args) {
    unsigned long long ptr;
    if (!PyArg_ParseTuple(args, "K", &ptr))
        return NULL;
    return Py_BuildValue("s", Object_GetClassName((Object*)ptr));
}

static PyObject* Py_Object_Delete(PyObject *self, PyObject *args) {
    unsigned long long ptr;
    if (!PyArg_ParseTuple(args, "K", &ptr))
        return NULL;
    Object_Delete((Object*)ptr);
    return Py_None;
}

static PyObject* Py_Application_New(PyObject *self, PyObject *args) {
    Application *app = Application_New();
    return Py_BuildValue("K", (unsigned long long)app);
}

static PyObject* Py_Application_Exec(PyObject *self, PyObject *args) {
    unsigned long long ptr;
    if (!PyArg_ParseTuple(args, "K", &ptr))
        return NULL;
    return Py_BuildValue("i", Application_Exec((Application*)ptr));
}

static PyObject* Py_VBoxLayout_New(PyObject *self, PyObject *args) {
    unsigned long long ptr;
    if (!PyArg_ParseTuple(args, "K", &ptr))
        return NULL;
    VBoxLayout *layout = VBoxLayout_New((Widget*)ptr);
    return Py_BuildValue("K", (unsigned long long)layout);
}

static PyObject* Py_Layout_AddWidget(PyObject *self, PyObject *args) {
    unsigned long long layout_ptr, widget_ptr;
    if (!PyArg_ParseTuple(args, "KK", &layout_ptr, &widget_ptr))
        return NULL;
    Layout_AddWidget((Layout*)layout_ptr, (Widget*)widget_ptr);
    return Py_None;
}

static PyObject* Py_Widget_New(PyObject *self, PyObject *args) {
    unsigned long long ptr = 0;
    if (!PyArg_ParseTuple(args, "|K", &ptr))
        return NULL;
    Widget *widget = Widget_New((Widget*)ptr);
    return Py_BuildValue("K", (unsigned long long)widget);
}

static PyObject* Py_Widget_SetVisible(PyObject *self, PyObject *args) {
    unsigned long long ptr;
    char visible;
    if (!PyArg_ParseTuple(args, "Kb", &ptr, &visible))
        return NULL;
    Widget_SetVisible((Widget*)ptr, visible);
    return Py_None;
}

static PyObject* Py_Widget_SetWindowTitle(PyObject *self, PyObject *args) {
    unsigned long long ptr;
    char *title;
    if (!PyArg_ParseTuple(args, "Ks", &ptr, &title))
        return NULL;
    Widget_SetWindowTitle((Widget*)ptr, title);
    return Py_None;
}

static PyObject* Py_Widget_SetLayout(PyObject *self, PyObject *args) {
    unsigned long long widget_ptr, layout_ptr;
    if (!PyArg_ParseTuple(args, "KK", &widget_ptr, &layout_ptr))
        return NULL;
    Widget_SetLayout((Widget*)widget_ptr, (Layout*)layout_ptr);
    return Py_None;
}

static PyObject* Py_Widget_SetSize(PyObject *self, PyObject *args) {
    unsigned long long ptr;
    int x, y;
    if (!PyArg_ParseTuple(args, "Kii", &ptr, &x, &y))
        return NULL;
    Widget_SetSize((Widget*)ptr, x, y);
    return Py_None;
}

static PyObject* Py_Label_New(PyObject *self, PyObject *args) {
    unsigned long long ptr;
    if (!PyArg_ParseTuple(args, "K", &ptr))
        return NULL;
    Label *label = Label_New((Widget*)ptr);
    return Py_BuildValue("K", (unsigned long long)label);
}

static PyObject* Py_Label_SetText(PyObject *self, PyObject *args) {
    unsigned long long ptr;
    char *text;
    if (!PyArg_ParseTuple(args, "Ks", &ptr, &text))
        return NULL;
    Label_SetText((Label*)ptr, text);
    return Py_None;
}

static PyObject* Py_PushButton_New(PyObject *self, PyObject *args) {
    unsigned long long ptr;
    if (!PyArg_ParseTuple(args, "K", &ptr))
        return NULL;
    PushButton *button = PushButton_New((Widget*)ptr);
    return Py_BuildValue("K", (unsigned long long)button);
}

static PyObject* Py_PushButton_SetText(PyObject *self, PyObject *args) {
    unsigned long long ptr;
    char *text;
    if (!PyArg_ParseTuple(args, "Ks", &ptr, &text))
        return NULL;
    PushButton_SetText((PushButton*)ptr, text);
    return Py_None;
}

static PyObject* Py_PushButton_SetOnClicked(PyObject *self, PyObject *args) {
    unsigned long long ptr;
    void *obj;
    if (!PyArg_ParseTuple(args, "KO", &ptr, &obj))
        return NULL;

    Py_INCREF(obj);

    PushButton *button = (PushButton*)ptr;
    auto callback_wrapper = [=](){
        if (PyCallable_Check((PyObject*)obj)) {
            PyObject_CallObject((PyObject*)obj, NULL);
        } else {
            fprintf(stderr, "Warning (exception was never cached): OnClicked callback is not callable\n");
        }
    };

    auto destroy_wrapper = [=](){
        Py_DECREF(obj);  // ensure python callback is destroyed whenever the button is destroyed
    };

    QObject::connect(button->ptr, &QPushButton::clicked, button->ptr, callback_wrapper);
    QObject::connect(button->ptr, &QPushButton::destroyed, button->ptr, destroy_wrapper);

    return Py_None;
}

static PyMethodDef _pywidgets_methods[] = {
    {"Object_GetClassName",     Py_Object_GetClassName,     METH_VARARGS, "GetClassName(class ptr) -> str"},
    {"Object_Delete",           Py_Object_Delete,           METH_VARARGS, "Object_Delete(class ptr) -> None"},
    {"Application_New",         Py_Application_New,         METH_NOARGS,  "Application_New() -> class ptr"},
    {"Application_Exec",        Py_Application_Exec,        METH_VARARGS, "Application_Exec(app ptr) -> None"},
    {"VBoxLayout_New",          Py_VBoxLayout_New,          METH_VARARGS, "VBoxLayout_New(parent ptr) -> class ptr"},
    {"Layout_AddWidget",        Py_Layout_AddWidget,        METH_VARARGS, "Layout_AddWidget(layout ptr, widget ptr) -> None"},
    {"Widget_New",              Py_Widget_New,              METH_VARARGS, "Widget_New(parent ptr) -> class ptr"},
    {"Widget_SetVisible",       Py_Widget_SetVisible,       METH_VARARGS, "Widget_SetVisible(widget ptr, bool) -> None"},
    {"Widget_SetWindowTitle",   Py_Widget_SetWindowTitle,   METH_VARARGS, "Widget_SetWindowTitle(widget ptr, str) -> None"},
    {"Widget_SetLayout",        Py_Widget_SetLayout,        METH_VARARGS, "Widget_SetLayout(widget ptr, layout ptr) -> None"},
    {"Widget_SetSize",          Py_Widget_SetSize,          METH_VARARGS, "Widget_SetSize(widget ptr, int, int) -> None"},
    {"Label_New",               Py_Label_New,               METH_VARARGS, "Label_New(parent ptr) -> class ptr"},
    {"Label_SetText",           Py_Label_SetText,           METH_VARARGS, "Label_SetText(label ptr, str) -> None"},
    {"PushButton_New",          Py_PushButton_New,          METH_VARARGS, "PushButton_New(parent ptr) -> class ptr"},
    {"PushButton_SetText",      Py_PushButton_SetText,      METH_VARARGS, "PushButton_SetText(button ptr, str) -> None"},
    {"PushButton_SetOnClicked", Py_PushButton_SetOnClicked, METH_VARARGS, "Py_PushButton_SetOnClicked(button ptr, callable) -> None"},
    {NULL, NULL, 0, NULL}
};

static char module_docstring[] = "It's like PyQt but WAY COOLER!!1";
static struct PyModuleDef _pywidgets_module = {
    PyModuleDef_HEAD_INIT,
    "_pywidgets",   /* name of module */
    module_docstring,
    -1, /* size of per-interpreter state of the module, or -1 if the module keeps state in global variables. */
    _pywidgets_methods
};

PyMODINIT_FUNC PyInit__pywidgets(void) {
    PyObject *module = PyModule_Create(&_pywidgets_module);
    return module;
}
