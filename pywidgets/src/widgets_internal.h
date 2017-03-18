//
// Created by Vladimir Goncharov on 02.02.17.
//

#ifndef PY_SIMPLE_WIDGETS_INTERNAL__H
#define PY_SIMPLE_WIDGETS_INTERNAL__H

#ifdef __cplusplus
extern "C" {
#endif

enum _classtype {
    _classtype_object,
    _classtype_application,
    _classtype_widget,
    _classtype_pushbutton,
    _classtype_label,
    _classtype_layout,
    _classtype_vboxlayout,
};

struct Object      {         void *ptr; enum _classtype classtype; };
struct Application { QApplication *ptr; enum _classtype classtype; };
struct Widget      {      QWidget *ptr; enum _classtype classtype; };
struct PushButton  {  QPushButton *ptr; enum _classtype classtype; };
struct Label       {       QLabel *ptr; enum _classtype classtype; };
struct Layout      {      QLayout *ptr; enum _classtype classtype; };
struct VBoxLayout  {  QVBoxLayout *ptr; enum _classtype classtype; };

#ifdef __cplusplus
} //extern "C"
#endif

#endif //PY_SIMPLE_WIDGETS_INTERNAL__H
