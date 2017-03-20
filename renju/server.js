const express = require('express');
const path = require('path');
const favicon = require('serve-favicon');
const logger = require('morgan');
const cookieParser = require('cookie-parser');
const bodyParser = require('body-parser');
const session = require('express-session');
const Knex = require('knex');

const configs = require('./knexfile');

const KnexSessionStore = require('connect-session-knex')(session);

const env = process.env.NODE_ENV || 'development';

const knex = Knex(configs[env]);

const sessionStore = new KnexSessionStore({
    knex: knex,
    tablename: 'sessions'
});

const sessionParser = session({
    resave: false,
    saveUninitialized: true,
    secret: 'keyboard cat',
    store: sessionStore
});

const app = express();
const http = require('http').Server(app);
const io = require('socket.io')(http);

// view engine setup
app.set('views', path.join(__dirname, 'views'));
app.set('view engine', 'hbs');

app.use(logger('dev'));
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: false }));
app.use(cookieParser());
app.use(sessionParser);
app.use(require('node-sass-middleware')({
    src: path.join(__dirname, 'public'),
    dest: path.join(__dirname, 'public'),
    indentedSyntax: true,
    sourceMap: true
}));
app.use(express.static(path.join(__dirname, 'public')));

app.use('/', require('./routes/game')(knex));

// catch 404 and forward to error handler
app.use((req, res, next) => {
    const err = new Error('Not Found');
    err.status = 404;
    next(err);
});

// error handler
app.use((err, req, res, next) => {
    // set locals, only providing error in development
    res.locals.message = err.message;
    res.locals.error = req.app.get('env') === 'development' ? err : {};

    // render the error page
    res.status(err.status || 500);
    res.render('error');
});

io.on('connection', require('./routes/game_ws')(io, knex, sessionParser));

module.exports = http;
