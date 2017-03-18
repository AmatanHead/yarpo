//
// Created by Vladimir Goncharov on 02.02.17.
//

#include <stdio.h>
#include <stdbool.h>
#include <assert.h>

#include <QCoreApplication>
#include <QApplication>
#include <QtWidgets>

#include "widgets.h"
#include "widgets_internal.h"


#ifdef __cplusplus
extern "C" {
#endif

static const char * const _classname_object =      (char*)"Object";
static const char * const _classname_application = (char*)"Application";
static const char * const _classname_widget =      (char*)"Widget";
static const char * const _classname_pushbutton =  (char*)"PushButton";
static const char * const _classname_label =       (char*)"Label";
static const char * const _classname_layout =      (char*)"Layout";
static const char * const _classname_vboxlayout =  (char*)"VBoxLayout";

const char *Object_GetClassName(struct Object *object) {
    assert(object != NULL);
    switch (object->classtype) {
        case _classtype_object:      return _classname_object;
        case _classtype_application: return _classname_application;
        case _classtype_widget:      return _classname_widget;
        case _classtype_pushbutton:  return _classname_pushbutton;
        case _classtype_label:       return _classname_label;
        case _classtype_layout:      return _classname_layout;
        case _classtype_vboxlayout:  return _classname_vboxlayout;
    }
    return NULL;
}

void Object_Delete(struct Object *object) {
    if (object != NULL) {
        switch (object->classtype) {
            case _classtype_object:                                            break;
            case _classtype_application: delete((QApplication*)(object->ptr)); break;
            case _classtype_widget:      delete(     (QWidget*)(object->ptr)); break;
            case _classtype_pushbutton:  delete( (QPushButton*)(object->ptr)); break;
            case _classtype_label:       delete(      (QLabel*)(object->ptr)); break;
            case _classtype_layout:      delete(     (QLayout*)(object->ptr)); break;
            case _classtype_vboxlayout:  delete( (QVBoxLayout*)(object->ptr)); break;
            default:                                                           break;
        }
        delete(object);
    }
}

struct Application *Application_New() {
    static int _argc = 0;
    static char **_argv = {};

    QApplication *q_application = new QApplication(_argc, _argv);
    Application *application = new Application();

    application->ptr = q_application;
    application->classtype = _classtype_application;

    return application;
}

int Application_Exec(struct Application *app) {
    assert(app != NULL);
    assert(app->ptr != NULL);
    return app->ptr->exec();
}

QWidget *_get_q_parent(struct Widget *parent) {
    if (parent == NULL) {
        return NULL;
    } else {
        return parent->ptr;
    }
}

struct VBoxLayout *VBoxLayout_New(struct Widget *parent) {
    QWidget *q_parent = _get_q_parent(parent);
    QVBoxLayout *q_v_box_layout = new QVBoxLayout(q_parent);
    VBoxLayout *v_box_layout = new VBoxLayout();

    v_box_layout->ptr = q_v_box_layout;
    v_box_layout->classtype = _classtype_vboxlayout;

    return v_box_layout;
}

void Layout_AddWidget(struct Layout *layout, struct Widget *widget) {
    assert(widget != NULL);
    assert(widget->ptr != NULL);
    assert(layout != NULL);
    assert(layout->ptr != NULL);
    layout->ptr->addWidget(widget->ptr);
}

struct Widget *Widget_New(struct Widget *parent) {
    QWidget *q_parent = _get_q_parent(parent);
    QWidget *q_widget = new QWidget(q_parent);
    Widget *widget = new Widget();

    widget->ptr = q_widget;
    widget->classtype = _classtype_widget;

    return widget;
}

void Widget_SetVisible(struct Widget *widget, bool v) {
    assert(widget != NULL);
    assert(widget->ptr != NULL);
    widget->ptr->setVisible(v);
}

void Widget_SetWindowTitle(struct Widget *widget, const char *title) {
    assert(widget != NULL);
    assert(widget->ptr != NULL);
    widget->ptr->setWindowTitle(title);
}

void Widget_SetLayout(struct Widget *widget, struct Layout *layout) {
    assert(widget != NULL);
    assert(widget->ptr != NULL);
    assert(layout != NULL);
    assert(layout->ptr != NULL);
    widget->ptr->setLayout(layout->ptr);
}

void Widget_SetSize(struct Widget *widget, int w, int h) {
    assert(widget != NULL);
    assert(widget->ptr != NULL);
    widget->ptr->setFixedSize(w, h);
}

struct Label *Label_New(struct Widget *parent) {
    QWidget *q_parent = _get_q_parent(parent);
    QLabel *q_label = new QLabel(q_parent);
    Label *label = new Label();

    label->ptr = q_label;
    label->classtype = _classtype_label;

    return label;
}

void Label_SetText(struct Label *label, const char *text) {
    assert(label != NULL);
    assert(label->ptr != NULL);
    label->ptr->setText(text);
}

struct PushButton *PushButton_New(struct Widget *parent) {
    QWidget *q_parent = _get_q_parent(parent);
    QPushButton *q_pushbutton = new QPushButton(q_parent);
    PushButton *pushbutton = new PushButton();

    pushbutton->ptr = q_pushbutton;
    pushbutton->classtype = _classtype_pushbutton;

    return pushbutton;
}

void PushButton_SetText(struct PushButton *button, const char *text) {
    assert(button != NULL);
    assert(button->ptr != NULL);
    button->ptr->setText(text);
}

void PushButton_SetOnClicked(struct PushButton *button, NoArgumentsCallback *callback) {
    assert(button != NULL);
    assert(button->ptr != NULL);
    auto callback_wrapper = [=](){ callback((struct Object*)button); };
    QObject::connect(button->ptr, &QPushButton::clicked, button->ptr, callback_wrapper);
}

#ifdef __cplusplus
}
#endif
