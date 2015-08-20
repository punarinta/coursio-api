<?php

include 'CoursioApi.php';

$api = new CoursioApi('', '');
$items = $api->exec('dashboard', 'read', null);

print_r($items);
