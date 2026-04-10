INSERT INTO training_module (title, description, category, difficulty, duration, active, order_index)
SELECT * FROM (VALUES
                   ('Comprendre la maladie d''Alzheimer', 'Introduction essentielle pour comprendre les symptômes, l''évolution et les défis liés à la maladie d''Alzheimer.', 'education', 'BEGINNER', 15, true, 1),
                   ('Gérer le stress de l''aidant', 'Apprenez à identifier les signes de surmenage et découvrez des stratégies pratiques pour préserver votre santé.', 'stress', 'BEGINNER', 20, true, 2),
                   ('Communication bienveillante', 'Techniques pour communiquer efficacement et avec empathie avec un patient atteint de troubles cognitifs.', 'communication', 'INTERMEDIATE', 25, true, 3),
                   ('Prévenir l''épuisement', 'Module approfondi sur les mécanismes de l''épuisement (burnout) chez les aidants.', 'stress', 'ADVANCED', 30, true, 4),
                   ('Les jeux de mémoire pour patients', 'Activités ludiques et jeux simples pour stimuler la mémoire de votre proche.', 'activities', 'BEGINNER', 20, true, 5),
                   ('Techniques de relaxation rapide', 'Exercices pratiques de respiration et de relaxation pour faire face au stress.', 'stress', 'INTERMEDIATE', 15, true, 6)
              ) AS tmp(title, description, category, difficulty, duration, active, order_index)
WHERE NOT EXISTS (SELECT 1 FROM training_module LIMIT 1);