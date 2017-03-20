const express = require("express");


function wrap(fn) {
    return (req, res, next) => {
        const routePromise = fn(req, res, next);
        if (routePromise.catch) {
            routePromise.catch(err => next(err));
        }
    }
}


module.exports = function (knex) {
    const router = express.Router();

    router.get('/', wrap(async(req, res) => {
        const myGames = await knex('games')
            .where('user1', req.sessionID)
            .orWhere('user2', req.sessionID)
            .select('id', 'user1', 'user2', 'status')
            .orderBy('id', 'desc');

        const newGames = await knex('games')
            .whereNull('user1')
            .orWhereNull('user2')
            .select('id', 'user1', 'user2', 'status')
            .orderBy('id', 'desc');

        res.render('index', { id: req.sessionID, myGames: myGames, newGames: newGames });
    }));

    router.get('/new', wrap(async(req, res) => {
        const newGame = await knex('games').insert({
            user1: req.sessionID,
            user2: null,
            status: 'New',
            data: JSON.stringify({
                moves: {},
                currentMove: 0,
                currentMovePlayer: Math.random() < 0.5 ? 'black' : 'white'
            })
        });

        res.redirect('game?id=' + newGame[0])
    }));

    router.get('/game', wrap(async(req, res, next) => {
        await knex.transaction(async function(trx) {
            // not supported by sqlite; however, we can't run more than one transaction with sqlite.
            // await trx.raw('set transaction isolation level repeatable read;');
            const games = await knex('games').transacting(trx).where('id', req.query.id);
            if (games.length == 0) {
                const err = new Error('Not Found');
                err.status = 404;
                next(err);
            } else if (games.length > 1) {
                console.error('multiple games with the same id ' + req.query.id);
                const err = new Error('Server error');
                err.status = 500;
                next(err);
            } else {
                const game = games[0];
                if (game.user2 === null && game.user1 !== req.sessionID) {
                    game.user2 = req.sessionID;
                    game.status = 'In progress';
                    await knex('games').transacting(trx).where('id', req.query.id).update(game);
                }
                res.status(200).render('game', {game: game})
            }
        })
    }));

    return router;
};
