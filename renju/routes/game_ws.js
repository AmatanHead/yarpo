const loadedGames = {};


module.exports = function (io, knex, sessionParser) {
    function onConnected(socket) {
        let game = null;

        function sendState() {
            if (game !== null) {
                io.to(game.id).emit('update', {
                    data: game.data,
                    user1: game.user1,
                    user2: game.user2,
                    status: game.status
                });
            }
        }

        function playerIdToColor(id) {
            if (game.user1 === id) {
                return 'white'
            } else if (game.user2 === id) {
                return 'black'
            } else {
                return null
            }
        }

        function generateKey(x, y) {
            return '' + x + ' ' + y;
        }

        function checkWin(x, y, moves) {
            const key = generateKey(x, y);
            const color = moves[key].color;

            let v = -1, h = -1, d1 = -1, d2 = -1;
            let cell;

            cell = moves[key];
            while (cell && cell.color == color) { cell = moves[generateKey(cell.x, cell.y + 1)]; v += 1; }
            cell = moves[key];
            while (cell && cell.color == color) { cell = moves[generateKey(cell.x + 1, cell.y)]; h += 1; }
            cell = moves[key];
            while (cell && cell.color == color) { cell = moves[generateKey(cell.x + 1, cell.y + 1)]; d1 += 1; }
            cell = moves[key];
            while (cell && cell.color == color) { cell = moves[generateKey(cell.x + 1, cell.y - 1)]; d2 += 1; }
            cell = moves[key];
            while (cell && cell.color == color) { cell = moves[generateKey(cell.x, cell.y - 1)]; v += 1; }
            cell = moves[key];
            while (cell && cell.color == color) { cell = moves[generateKey(cell.x - 1, cell.y)]; h += 1; }
            cell = moves[key];
            while (cell && cell.color == color) { cell = moves[generateKey(cell.x - 1, cell.y - 1)]; d1 += 1; }
            cell = moves[key];
            while (cell && cell.color == color) { cell = moves[generateKey(cell.x - 1, cell.y + 1)]; d2 += 1; }

            return v >= 5 || h >= 5 || d1 >= 5 || d2 >= 5
        }

        function saveGame(gameId) {
            if (loadedGames.hasOwnProperty(gameId)) {
                if (loadedGames[gameId] !== null) {
                    knex('games').where('id', gameId).update({
                        user1: loadedGames[gameId].user1,
                        user2: loadedGames[gameId].user2,
                        data: JSON.stringify(loadedGames[gameId].data),
                        status: loadedGames[gameId].status,
                    }).then(() => {
                        if (loadedGames.hasOwnProperty(gameId) && loadedGames[gameId].refs === 0) {
                            delete loadedGames[gameId];
                        }
                    });
                }
            }
        }

        sessionParser(socket.client.request, {}, () => {
            socket.on('handshake', (gameId) => {
                if (game !== null) {
                    socket.emit('err', 'Double handshake');
                }

                knex('games').where('id', gameId)
                    .then((games) => {
                        if (games.length == 0) {
                            socket.emit('err', 'No game with such id');
                        } else if (games.length > 1) {
                            console.error('multiple games with the same id ' + gameId);
                            socket.emit('err', 'Multiple games with the same id');
                        } else {
                            if (loadedGames.hasOwnProperty(gameId)) {
                                game = loadedGames[gameId];
                                game.refs += 1;

                                if (games[0].status === 'In progress' && game.status === 'New') {
                                    game.data = JSON.parse(games[0].data);
                                    game.status = games[0].status;
                                    game.user1 = games[0].user1;
                                    game.user2 = games[0].user2;
                                }
                            } else {
                                game = games[0];
                                game.data = JSON.parse(game.data);
                                game.refs = 1;
                                loadedGames[gameId] = game;
                            }

                            socket.emit('handshake response', {
                                color: playerIdToColor(socket.client.request.sessionID),
                                status: game.status
                            });
                        }

                        socket.join(gameId);

                        sendState();
                    })
                    .catch((err) => {
                        console.error(err.message, err);
                        socket.emit('err', 'Internal server error');
                    });
            });

            socket.on('disconnect', () => {
                if (game !== null) {
                    game.refs -= 1;
                    saveGame(game.id);
                    game = null;
                }
            });

            socket.on('move', (data) => {
                if (game === null) {
                    socket.emit('err', 'Move before handshake');
                    return;
                }

                if (typeof data.x !== 'number' || typeof data.y !== 'number') {
                    socket.emit('err', 'Coordinates are not numeric');
                    return;
                }

                if (game.status != 'In progress') {
                    return;
                }

                if (data.currentMove !== game.data.currentMove) {
                    socket.emit('err', 'Outdated state');
                    return;
                }

                if (playerIdToColor(socket.client.request.sessionID) !== game.data.currentMovePlayer) {
                    socket.emit('err', 'Not your turn');
                    return;
                }

                const key = generateKey(data.x, data.y);

                if (game.data.moves.hasOwnProperty(key)) {
                    socket.emit('err', 'This cell is not empty');
                    return;
                }

                game.data.currentMove += 1;

                game.data.moves[key] = {
                    x: data.x,
                    y: data.y,
                    move: game.data.currentMove,
                    color: game.data.currentMovePlayer
                };

                if (checkWin(data.x, data.y, game.data.moves)) {
                    game.status = 'Finished';
                    game.data.moves[key].lastMove = true;
                }

                game.data.currentMovePlayer = game.data.currentMovePlayer === 'white' ? 'black' : 'white';

                sendState();

                saveGame(game.id);
            });
        });
    }

    return onConnected;
};
